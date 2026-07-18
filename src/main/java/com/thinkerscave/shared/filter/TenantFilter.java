package com.thinkerscave.shared.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.security.service.JwtService;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.exceptions.ApiError;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Resolves tenant for schema-per-tenant routing.
 *
 * <p><b>Authenticated requests</b> (Bearer present): tenant MUST come from the JWT
 * {@code tenant} claim. {@code X-Tenant-ID} is rejected if it mismatches.
 *
 * <p><b>Pre-auth requests</b> (login / public): subdomain or {@code X-Tenant-ID} is allowed.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEFAULT_TENANT = "public";

    private final JwtService jwtService;
    private final SubdomainTenantResolver subdomainResolver;
    private final ObjectMapper objectMapper;

    @Value("${app.multi-tenancy.enabled:true}")
    private boolean multiTenancyEnabled;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            if (!multiTenancyEnabled) {
                TenantContext.setTenant(DEFAULT_TENANT);
                filterChain.doFilter(request, response);
                return;
            }

            String bearerToken = extractBearerToken(request);
            String headerTenant = normalizeTenant(request.getHeader(TENANT_HEADER));

            if (bearerToken != null) {
                String jwtTenant;
                Set<String> switchableTenants = Set.of();
                try {
                    var claims = jwtService.extractAllClaims(bearerToken);
                    jwtTenant = extractTenantFromClaims(claims.get("tenant"));
                    switchableTenants = extractSwitchableTenants(claims.get("switchableTenants"));
                } catch (JwtException | IllegalArgumentException ex) {
                    // Let JwtAuthenticationFilter produce the auth error; still set a safe default.
                    TenantContext.setTenant(DEFAULT_TENANT);
                    filterChain.doFilter(request, response);
                    return;
                }

                if (!StringUtils.hasText(jwtTenant)) {
                    sendForbidden(response, "Authenticated token is missing tenant claim");
                    return;
                }

                jwtTenant = normalizeTenant(jwtTenant);
                if (StringUtils.hasText(headerTenant) && !headerTenant.equalsIgnoreCase(jwtTenant)) {
                    if (!switchableTenants.contains(normalizeTenant(headerTenant))) {
                        log.warn("Tenant spoofing rejected: header={} jwtTenant={} path={}",
                                headerTenant, jwtTenant, request.getRequestURI());
                        sendForbidden(response, "X-Tenant-ID does not match authenticated tenant");
                        return;
                    }
                    TenantContext.setTenant(normalizeTenant(headerTenant));
                } else {
                    TenantContext.setTenant(jwtTenant);
                }
            } else {
                // Pre-authentication: subdomain → header → default (login / public flows)
                String tenant = null;
                try {
                    tenant = normalizeTenant(subdomainResolver.extractTenantFromSubdomain(request));
                } catch (Exception e) {
                    log.trace("Subdomain extraction failed: {}", e.getMessage());
                }
                if (!StringUtils.hasText(tenant)) {
                    tenant = headerTenant;
                }
                TenantContext.setTenant(StringUtils.hasText(tenant) ? tenant : DEFAULT_TENANT);
            }

            log.trace("Tenant resolved: {}", TenantContext.getTenant());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX) && authHeader.length() > BEARER_PREFIX.length()) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private String extractTenantFromClaims(Object tenantClaim) {
        return tenantClaim != null ? tenantClaim.toString() : null;
    }

    private Set<String> extractSwitchableTenants(Object raw) {
        if (!(raw instanceof Collection<?> values)) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (Object value : values) {
            String tenant = normalizeTenant(value != null ? value.toString() : null);
            if (tenant != null) {
                normalized.add(tenant);
            }
        }
        return normalized;
    }

    private String normalizeTenant(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim().toLowerCase().replace('-', '_');
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .code("TENANT_MISMATCH")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
