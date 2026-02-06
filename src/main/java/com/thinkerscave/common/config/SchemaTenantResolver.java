package com.thinkerscave.common.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * SchemaTenantResolver provides the current tenant identifier
 * to Hibernate during session creation.
 *
 * This allows Hibernate to dynamically choose the schema/database
 * based on the tenant set by the TenantFilter.
 */
@Component
@Slf4j
public class SchemaTenantResolver implements CurrentTenantIdentifierResolver<String> {

    private static final String DEFAULT_TENANT = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getTenant();
        log.debug("DEBUG: Resolve Tenant: {}", tenant);
        return (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}