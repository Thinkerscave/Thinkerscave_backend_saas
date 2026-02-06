package com.thinkerscave.common.config;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SchemaMultiTenantConnectionProvider implements Hibernate's
 * MultiTenantConnectionProvider.
 *
 * It provides JDBC connections scoped to a specific schema (tenant) using
 * PostgreSQL's
 * `SET search_path` mechanism, enabling schema-based multi-tenancy.
 */
@Component
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    @Autowired
    private DataSource dataSource;

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return unwrapType.isAssignableFrom(getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        return (T) this;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    /**
     * Provides a tenant-specific connection by setting the schema dynamically.
     * Uses PostgreSQL's 'SET search_path' to switch schemas.
     */
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        final Connection connection = getAnyConnection();
        // Sanitize and set schema
        String schema = sanitizeSchemaName(tenantIdentifier);
        connection.createStatement().execute("SET search_path TO \"" + schema + "\"");
        return connection;
    }

    /**
     * Cleans up after tenant-specific usage.
     * Resets schema to 'public' to avoid leaking tenant context.
     */
    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.createStatement().execute("SET search_path TO public");
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    /**
     * Sanitizes schema name to prevent SQL injection.
     */
    private String sanitizeSchemaName(String schema) {
        if (schema == null)
            return "public";
        return schema.replaceAll("[^a-zA-Z0-9_]", "");
    }
}