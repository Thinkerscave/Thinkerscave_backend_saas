package com.thinkerscave.shared.context;

/**
 * TenantContext — thread-local tenant identifier.
 * Used by multi-tenant filters and Hibernate schema routing.
 */
public final class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenant(String tenant) {
        currentTenant.set(tenant);
    }

    public static String getTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
