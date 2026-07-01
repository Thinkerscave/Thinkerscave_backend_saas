package com.thinkerscave.shared.filter;

import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MdcFilter — Order 4.
 *
 * Runs last in the filter chain. Populates the SLF4J MDC with diagnostic
 * context so every log line carries a consistent correlation ID, tenant,
 * org, user, path, and method.
 *
 * The correlation ID is also echoed back in the {@code X-Correlation-Id}
 * response header so clients can include it in bug reports.
 */
@Component
@Order(4)
@Slf4j
public class MdcFilter extends OncePerRequestFilter {

    public static final String MDC_CORRELATION_ID  = "correlationId";
    public static final String MDC_TENANT_ID        = "tenantId";
    public static final String MDC_ORG_ID           = "organizationId";
    public static final String MDC_USER_ID          = "userId";
    public static final String MDC_REQUEST_PATH     = "requestPath";
    public static final String MDC_REQUEST_METHOD   = "requestMethod";
    public static final String CORRELATION_HEADER   = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Correlation ID — reuse from client or generate
            String correlationId = request.getHeader(CORRELATION_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString().substring(0, 8);
            }
            MDC.put(MDC_CORRELATION_ID, correlationId);
            response.setHeader(CORRELATION_HEADER, correlationId);

            // Tenant
            String tenant = TenantContext.getTenant();
            if (tenant != null) MDC.put(MDC_TENANT_ID, tenant);

            // Org
            Long orgId = OrganizationContext.getOrganizationId();
            if (orgId != null) MDC.put(MDC_ORG_ID, String.valueOf(orgId));

            // Authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                MDC.put(MDC_USER_ID, auth.getName());
            }

            MDC.put(MDC_REQUEST_PATH,   request.getRequestURI());
            MDC.put(MDC_REQUEST_METHOD, request.getMethod());

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
