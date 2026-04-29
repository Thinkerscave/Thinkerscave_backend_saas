package com.thinkerscave.common.auditing.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.common.auditing.annotation.Auditable;
import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP Aspect for automatic audit logging.
 * 
 * Intercepts methods annotated with @Auditable and records:
 * - Who (authenticated user)
 * - What (action performed)
 * - When (timestamp)
 * - Where (tenant/organization context)
 * - Duration (execution time)
 * - Result (success/failure)
 * 
 * @author System
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Around advice for @Auditable methods.
     * Captures method execution and logs to audit table.
     */
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        String tenantId = TenantContext.getCurrentTenant();
        Long organizationId = OrganizationContext.getOrganizationId();
        String performedBy = getAuthenticatedUser();
        String methodName = joinPoint.getSignature().toShortString();

        String action = auditable.action();
        String description = auditable.description().isEmpty() ? methodName : auditable.description();

        Map<String, Object> params = new HashMap<>();
        if (auditable.logParams()) {
            params = captureParameters(joinPoint, auditable.excludeParams());
        }

        String status = "SUCCESS";
        String errorMessage = null;
        Object result;

        try {
            // Execute the actual method
            result = joinPoint.proceed();

            // Optionally log result
            if (auditable.logResult() && result != null) {
                params.put("_result", sanitizeForLogging(result));
            }

            return result;

        } catch (Throwable e) {
            status = "FAILED";
            errorMessage = e.getMessage() != null ? e.getMessage().substring(0, Math.min(500, e.getMessage().length()))
                    : "Unknown error";
            throw e;

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log to database asynchronously (fire-and-forget)
            try {
                saveAuditLog(
                        tenantId, organizationId, action, description,
                        performedBy, params, status, errorMessage, duration);
            } catch (Exception ex) {
                // Don't fail the main operation if audit logging fails
                log.error("Failed to save audit log for action: {}", action, ex);
            }

            log.debug("Audit: {} by {} in tenant={} org={} - {} ({}ms)",
                    action, performedBy, tenantId, organizationId, status, duration);
        }
    }

    /**
     * Get the currently authenticated user.
     */
    private String getAuthenticatedUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.debug("Could not determine authenticated user", e);
        }
        return "SYSTEM";
    }

    /**
     * Capture method parameters while excluding sensitive ones.
     */
    private Map<String, Object> captureParameters(ProceedingJoinPoint joinPoint, String[] excludeParams) {
        Map<String, Object> params = new HashMap<>();

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] paramValues = joinPoint.getArgs();

            if (paramNames != null && paramValues != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    String name = paramNames[i];

                    // Skip excluded parameters
                    if (Arrays.asList(excludeParams).stream()
                            .anyMatch(ex -> name.toLowerCase().contains(ex.toLowerCase()))) {
                        params.put(name, "[REDACTED]");
                    } else {
                        params.put(name, sanitizeForLogging(paramValues[i]));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not capture parameters", e);
        }

        return params;
    }

    /**
     * Sanitize objects for JSON logging.
     */
    private Object sanitizeForLogging(Object value) {
        if (value == null)
            return null;

        // For complex objects, just log the class name and hashCode
        if (value.getClass().getPackage() != null
                && value.getClass().getPackage().getName().startsWith("com.thinkerscave")) {
            return value.getClass().getSimpleName() + "@" + Integer.toHexString(value.hashCode());
        }

        // For primitives and common types, log directly
        return value.toString();
    }

    /**
     * Save audit log to database.
     */
    private void saveAuditLog(String tenantId, Long organizationId, String action,
            String description, String performedBy, Map<String, Object> details,
            String status, String errorMessage, long durationMs) {

        String sql = """
                INSERT INTO organization_audit_log
                (tenant_id, organization_id, action, description, performed_by, details,
                 status, error_message, duration_ms, created_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        try {
            String detailsJson = objectMapper.writeValueAsString(details);

            jdbcTemplate.update(sql,
                    tenantId, organizationId, action, description, performedBy,
                    detailsJson, status, errorMessage, durationMs);
        } catch (Exception e) {
            log.error("Failed to insert audit log: {}", e.getMessage());
        }
    }
}
