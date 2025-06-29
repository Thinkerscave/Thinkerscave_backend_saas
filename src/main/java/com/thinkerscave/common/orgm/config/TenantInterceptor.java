package com.thinkerscave.common.orgm.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts HTTP requests to extract tenant identifier from headers
 * and sets it into the ThreadLocal context.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    /**
     * Executed before the controller method.
     * Extracts the tenant ID from the header and stores it.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenant = request.getHeader("X-Tenant-ID");
        if (tenant != null && !tenant.isEmpty()) {
            TenantContext.setTenant(tenant);
        }
        return true;
    }

    /**
     * Executed after request completion.
     * Clears the tenant context to prevent memory leaks.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
