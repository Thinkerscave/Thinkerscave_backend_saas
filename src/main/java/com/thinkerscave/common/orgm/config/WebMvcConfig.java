package com.thinkerscave.common.orgm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfig registers the TenantInterceptor in the Spring MVC interceptor chain.
 *
 * Interceptors in Spring MVC allow pre- and post-processing of HTTP requests.
 * In a multi-tenant architecture, this is typically used to extract the tenant identifier
 * from incoming requests (e.g., headers, subdomains) and store it in TenantContext.
 */

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // Injects the custom interceptor responsible for resolving and setting tenant context
    private final TenantInterceptor tenantInterceptor;

    // Constructor-based injection of the TenantInterceptor
    public WebMvcConfig(TenantInterceptor tenantInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
    }

    /**
     * Registers the TenantInterceptor with the Spring MVC request handling pipeline.
     * This ensures tenant resolution is performed before controller methods are invoked.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor);
    }
}