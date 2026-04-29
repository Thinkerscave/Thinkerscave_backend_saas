package com.thinkerscave.common.context;

/**
 * Thread-local context for storing the current organization ID.
 * Similar to TenantContext, but for organization-scoped operations.
 * 
 * @author System
 */
public class OrganizationContext {

    private static final ThreadLocal<Long> currentOrganizationId = new ThreadLocal<>();

    /**
     * Set the current organization ID for this thread.
     * 
     * @param orgId The organization ID to set
     */
    public static void setOrganizationId(Long orgId) {
        currentOrganizationId.set(orgId);
    }

    /**
     * Get the current organization ID for this thread.
     * 
     * @return The current organization ID, or null if not set
     */
    public static Long getOrganizationId() {
        return currentOrganizationId.get();
    }

    /**
     * Clear the organization context for this thread.
     * IMPORTANT: Always call this in a finally block to prevent memory leaks.
     */
    public static void clear() {
        currentOrganizationId.remove();
    }

    /**
     * Check if an organization context is currently set.
     * 
     * @return true if organization ID is set, false otherwise
     */
    public static boolean isSet() {
        return currentOrganizationId.get() != null;
    }
}
