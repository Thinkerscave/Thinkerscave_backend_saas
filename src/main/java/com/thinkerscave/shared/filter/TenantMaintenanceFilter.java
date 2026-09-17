package com.thinkerscave.shared.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.exceptions.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class TenantMaintenanceFilter extends OncePerRequestFilter {
    private static final List<String> EXEMPT_PREFIXES = List.of(
            "/api/auth/", "/api/v1/public/", "/api/platform/", "/actuator/");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.tenancy.platform-schema:public}")
    private String platformSchema;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXEMPT_PREFIXES.stream().anyMatch(request.getRequestURI()::startsWith);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String tenant = TenantContext.getTenant();
        if (tenant == null || "public".equalsIgnoreCase(tenant)) {
            chain.doFilter(request, response);
            return;
        }
        if (!platformSchema.matches("[A-Za-z0-9_]+")) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
            return;
        }
        Boolean maintenance = jdbcTemplate.query("""
                SELECT maintenance_mode FROM "%s".tenant_registry
                WHERE lower(replace(tenant_identifier, '-', '_'))=? AND active=true
                """.formatted(platformSchema),
                rs -> rs.next() && rs.getBoolean(1), tenant.toLowerCase().replace('-', '_'));
        if (!Boolean.TRUE.equals(maintenance)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .code("TENANT_MAINTENANCE")
                .message("This tenant is temporarily unavailable for maintenance")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
