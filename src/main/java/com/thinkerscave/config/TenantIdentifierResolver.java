package com.thinkerscave.config;

import com.thinkerscave.shared.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Resolves the active tenant identifier for Hibernate DATABASE multi-tenancy.
 *
 * <p>The identifier is read from {@link TenantContext}, which is populated by
 * {@code TenantFilter} at the start of every HTTP request (before Hibernate runs).
 *
 * <p>Returns {@code "public"} as the fallback — mapped to the platform schema
 * {@code thinkerscave_dev} by {@link TenantConnectionProvider}.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private static final String DEFAULT_TENANT = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getTenant();
        return (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
    }

    /**
     * Returns {@code true} so Hibernate validates existing sessions against the current
     * tenant, preventing stale connections from leaking across requests.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
