package com.thinkerscave.common.config;

import com.thinkerscave.common.multitenancy.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**

 - SchemaTenantResolver provides the current tenant identifier
 - to Hibernate during session creation.
 -
 - This allows Hibernate to dynamically choose the schema/database
 - based on the tenant set by the TenantFilter.
 */
@Component
public class SchemaTenantResolver implements CurrentTenantIdentifierResolver {

    private static final String DEFAULT_TENANT = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        String resolvedTenant = (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
//        System.out.println("Resolving tenant (organisation): " + resolvedTenant);
        return resolvedTenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}