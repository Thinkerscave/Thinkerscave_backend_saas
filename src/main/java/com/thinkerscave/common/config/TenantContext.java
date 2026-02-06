package com.thinkerscave.common.config;

/**
 * TenantContext stores and manages the current tenant identifier at the thread
 * level.
 *
 * It uses ThreadLocal to ensure that tenant information is scoped per
 * request/thread,
 * which is crucial in multi-tenant applications where different users may
 * belong to
 * different tenants concurrently.
 */
public class TenantContext {

    // Thread-local variable to hold the tenant identifier for the current
    // thread/request
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    // Sets the current tenant ID (typically extracted from a header or context)
    public static void setTenant(String tenant) {
        currentTenant.set(tenant);
    }

    // Retrieves the current thread-bound tenant identifier
    public static String getTenant() {
        return currentTenant.get();
    }

    // Clears the tenant context after the request is processed
    public static void clear() {
        currentTenant.remove();
    }

    // Alias for getTenant() – used interchangeably for readability
    public static String getCurrentTenant() {
        return currentTenant.get();
    }
}