package com.thinkerscave.shared.context;

/**
 * OrganizationContext — thread-local holder for the current organization ID.
 * Set by {@link com.thinkerscave.shared.filter.OrganizationFilter} after
 * authentication and cleared at the end of each request.
 */
public final class OrganizationContext {

    private static final ThreadLocal<Long> currentOrg = new ThreadLocal<>();

    private OrganizationContext() {}

    public static void setOrganizationId(Long organizationId) {
        currentOrg.set(organizationId);
    }

    public static Long getOrganizationId() {
        return currentOrg.get();
    }

    public static void clear() {
        currentOrg.remove();
    }
}
