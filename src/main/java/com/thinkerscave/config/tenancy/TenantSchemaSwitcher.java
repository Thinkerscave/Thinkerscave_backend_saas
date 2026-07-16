package com.thinkerscave.config.tenancy;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstracts schema-per-tenant catalog/schema switching so MySQL (dev) and
 * PostgreSQL (future prod) can share the same Hibernate multi-tenant provider.
 */
public interface TenantSchemaSwitcher {

    /**
     * Switches the connection to the physical schema/catalog for the given tenant slug.
     */
    void switchToTenant(Connection connection, String tenantIdentifier) throws SQLException;

    /**
     * Resets the connection to the platform/default schema before returning it to the pool.
     */
    void resetToPlatform(Connection connection) throws SQLException;

    /**
     * Maps a logical tenant identifier to a physical schema/catalog name.
     */
    String resolvePhysicalSchema(String tenantIdentifier);
}
