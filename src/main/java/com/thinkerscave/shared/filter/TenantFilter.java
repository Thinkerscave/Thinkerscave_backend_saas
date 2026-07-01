package com.thinkerscave.shared.filter;

import com.thinkerscave.security.service.JwtService;
import com.thinkerscave.shared.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TenantFilter — Order 1.
 *
 * Resolves the current tenant slug and stores it in {@link TenantContext}.
 * Detection priority (highest → lowest):
 * <ol>
 *   <li>Subdomain from hostname (production SaaS)</li>
 *   <li>{@code X-Tenant-ID} request header</li>
 *   <li>{@code tenant} claim embedded in JWT Bearer token</li>
 *   <li>Default "public" fallback</li>
 * </ol>
 *
 * When {@code app.multi-tenancy.enabled=false} (dev / H2 mode) the filter
 * is a no-op and always sets the "public" default tenant.
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

            String tenant = null;

            // Priority 1: subdomain
            try {
                tenant = subdomainResolver.extractTenantFromSubdomain(request);
            } catch (Exception e) {
                log.trace("Subdomain extraction failed: {}", e.getMessage());
            }

            // Priority 2: X-Tenant-ID header
            if (tenant == null || tenant.isBlank()) {
                String header = request.getHeader(TENANT_HEADER);
                if (header != null && !header.isBlank()) {
                    tenant = header.trim().toLowerCase().replace('-', '_');
                }
            }

            // Priority 3: tenant claim in JWT
            if (tenant == null || tenant.isBlank()) {
                String authHeader = request.getHeader(AUTH_HEADER);
                if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                    try {
                        String token = authHeader.substring(BEARER_PREFIX.length());
                        // Attempt to extract a "tenant" claim if present
                        var claims = jwtService.extractAllClaims(token);
                        Object tenantClaim = claims.get("tenant");
                        if (tenantClaim != null) {
                            tenant = tenantClaim.toString();
                        }
                    } catch (Exception e) {
                        log.trace("JWT tenant extraction failed: {}", e.getMessage());
                    }
                }
            }

            TenantContext.setTenant(tenant != null && !tenant.isBlank() ? tenant : DEFAULT_TENANT);
            log.trace("Tenant resolved: {}", TenantContext.getTenant());

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
