package com.thinkerscave.common.filter;

import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.resolver.SubdomainTenantResolver;
import com.thinkerscave.common.usrm.service.impl.JwtServiceImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TenantFilter - Extracts tenant identifier from subdomain, JWT, or HTTP
 * headers.
 * 
 * CRITICAL: This filter MUST run BEFORE JwtAuthFilter to ensure
 * the database queries during authentication go to the correct schema.
 * 
 * Execution Order:
 * 1. TenantFilter (sets TenantContext from subdomain/JWT/header)
 * 2. JwtAuthFilter (validates JWT, queries tenant-specific schema)
 * 3. Other Security Filters
 * 4. Controller
 * 
 * Priority for Tenant Detection (checked in order):
 * 0. Subdomain (sjcollege.thinkerscave.com) - Production, best UX
 * 1. JWT Token (embedded tenant claim) - Secure, for API calls
 * 2. X-Tenant-ID Header - Fallback for testing/backward compatibility
 * 3. Default "public" - For unauthenticated requests
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

    private final JwtServiceImpl jwtService;
    private final SubdomainTenantResolver subdomainResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String tenant = null;

            // Priority 0: Extract tenant from subdomain (production mode - best UX)
            try {
                tenant = subdomainResolver.extractTenantFromSubdomain(request);
                if (tenant != null && !tenant.isBlank()) {
                    log.debug("Tenant extracted from subdomain: {}", tenant);
                }
            } catch (Exception e) {
                log.debug("Could not extract tenant from subdomain: {}", e.getMessage());
                // Continue to fallback options
            }

            // Priority 1: Extract tenant from JWT token (authenticated API calls)
            if (tenant == null || tenant.isBlank()) {
                String authHeader = request.getHeader(AUTH_HEADER);
                if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                    String token = authHeader.substring(BEARER_PREFIX.length());
                    try {
                        // Use the public method to extract all claims to avoid double-parsing
                        io.jsonwebtoken.Claims claims = jwtService.extractAllClaimsPublic(token);
                        tenant = claims.get("tenant_id", String.class);
                        if (tenant != null && !tenant.isBlank()) {
                            log.debug("Tenant extracted from JWT: {}", tenant);
                        }
                        
                        // Cache the username in request attributes for JwtAuthFilter
                        String username = claims.getSubject();
                        if (username != null) {
                            request.setAttribute("JWT_USERNAME", username);
                        }
                    } catch (Exception e) {
                        log.debug("Could not extract tenant from JWT: {}", e.getMessage());
                        // Continue to fallback options
                    }
                }
            }

            // Priority 2: Fallback to X-Tenant-ID header (testing/backward compatibility)
            if (tenant == null || tenant.isBlank()) {
                tenant = request.getHeader(TENANT_HEADER);
                if (tenant != null && !tenant.isBlank()) {
                    log.debug("Tenant extracted from header: {}", tenant);
                }
            }

            // Priority 3: Default to public schema
            if (tenant == null || tenant.isBlank()) {
                tenant = DEFAULT_TENANT;
                log.debug("Using default tenant: {}", DEFAULT_TENANT);
            }

            // Sanitize and set tenant context
            tenant = sanitizeTenantName(tenant);
            TenantContext.setTenant(tenant);

            log.debug("Request tenant: {} | Path: {} | Method: {}",
                    tenant, request.getRequestURI(), request.getMethod());

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally

        {
            // CRITICAL: Always clear context to prevent memory leaks and cross-request
            // pollution
            TenantContext.clear();
        }
    }

    /**
     * Sanitizes tenant name to prevent SQL injection attacks.
     * Only allows alphanumeric characters and underscores.
     */
    private String sanitizeTenantName(String tenant) {
        if (tenant == null) {
            return DEFAULT_TENANT;
        }
        // Remove any characters that aren't alphanumeric or underscore
        String sanitized = tenant.replaceAll("[^a-zA-Z0-9_]", "");
        return sanitized.isEmpty() ? DEFAULT_TENANT : sanitized;
    }
}
