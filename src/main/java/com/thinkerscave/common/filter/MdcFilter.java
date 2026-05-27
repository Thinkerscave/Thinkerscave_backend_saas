package com.thinkerscave.common.filter;

import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.context.OrganizationContext;
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
 * MDC filter — populates SLF4J / Log4j2 MDC with per-request diagnostic
 * context so every log line and {@link com.thinkerscave.common.dto.ApiResponse}
 * envelope carries a consistent correlation id.
 *
 * <p>MDC keys populated:
 * <ul>
 *   <li>{@code correlationId} — UUID prefix (8 chars), or the inbound
 *       {@code X-Correlation-Id} header if present.</li>
 *   <li>{@code tenantId} — current tenant from {@link TenantContext}.</li>
 *   <li>{@code organizationId} — current organization from
 *       {@link OrganizationContext}.</li>
 *   <li>{@code userId} — authenticated principal name (if any).</li>
 *   <li>{@code requestPath}, {@code requestMethod} — for log forensics.</li>
 * </ul>
 *
 * <p>The correlation id is echoed back to the client in the
 * {@code X-Correlation-Id} response header so clients can include it in bug
 * reports.
 *
 * <p>Order: 4 — after {@link TenantFilter} (1), {@code JwtAuthFilter} (2) and
 * {@link OrganizationFilter} (3) so all contexts are populated.
 */
@Component
@Order(4)
@Slf4j
public class MdcFilter extends OncePerRequestFilter {

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_ORG_ID = "organizationId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_REQUEST_PATH = "requestPath";
    public static final String MDC_REQUEST_METHOD = "requestMethod";

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String correlationId = request.getHeader(CORRELATION_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString().substring(0, 8);
            }
            MDC.put(MDC_CORRELATION_ID, correlationId);
            response.setHeader(CORRELATION_HEADER, correlationId);

            String tenant = TenantContext.getTenant();
            if (tenant != null) MDC.put(MDC_TENANT_ID, tenant);

            Long orgId = OrganizationContext.getOrganizationId();
            if (orgId != null) MDC.put(MDC_ORG_ID, String.valueOf(orgId));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                MDC.put(MDC_USER_ID, auth.getName());
            }

            MDC.put(MDC_REQUEST_PATH, request.getRequestURI());
            MDC.put(MDC_REQUEST_METHOD, request.getMethod());

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
