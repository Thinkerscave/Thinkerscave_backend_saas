package com.thinkerscave.common.multitenancy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Thread-local based holder for the current tenant's schema name.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final String DEFAULT_TENANT = "public";

    /**
     * Set the current tenant's schema name.
     * @param tenantId the tenant identifier (schema name)
     */
    public static void setCurrentTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            currentTenant.set(DEFAULT_TENANT);
        } else {
            currentTenant.set(tenantId);
        }
    }

    /**
     * Get the current tenant's schema name.
     * @return the current tenant's schema name
     */
    public static String getCurrentTenant() {
        String tenant = currentTenant.get();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }

    /**
     * Clear the current tenant's context.
     */
    public static void clear() {
        currentTenant.remove();
    }
}
