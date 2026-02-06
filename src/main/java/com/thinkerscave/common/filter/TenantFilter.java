package com.thinkerscave.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.thinkerscave.common.config.TenantContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * TenantFilter - Extracts tenant identifier from HTTP headers.
 * 
 * CRITICAL: This filter MUST run BEFORE JwtAuthFilter to ensure
 * the database queries during authentication go to the correct schema.
 * 
 * Execution Order:
 * 1. TenantFilter (sets TenantContext)
 * 2. JwtAuthFilter (validates JWT, queries tenant-specific schema)
 * 3. Other Security Filters
 * 4. Controller
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT = "public";

    public TenantFilter() {
        log.info("TenantFilter Initialized");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract tenant from header
            String tenant = request.getHeader(TENANT_HEADER);
            log.debug("TenantFilter Header: {}", tenant);

            if (tenant != null && !tenant.isBlank()) {
                // Sanitize tenant name to prevent SQL injection via schema names
                tenant = sanitizeTenantName(tenant);
                TenantContext.setTenant(tenant);
            } else {
                // Default to public schema for unauthenticated requests (login, registration)
                TenantContext.setTenant(DEFAULT_TENANT);
            }

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
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
        // Remove any characters that aren't alphanumeric or underscore
        return tenant.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
