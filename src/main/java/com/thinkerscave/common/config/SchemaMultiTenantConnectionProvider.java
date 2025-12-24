package com.thinkerscave.common.config;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SchemaMultiTenantConnectionProvider implements Hibernate's MultiTenantConnectionProvider.
 *
 * It provides JDBC connections scoped to a specific schema (tenant) using PostgreSQL's
 * `SET search_path` mechanism, enabling schema-based multi-tenancy.
 */

@Component
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider {

    // Injects the application's shared DataSource (typically a connection pool)
    @Autowired
    private DataSource dataSource;

    // Hibernate internal method for unwrapping native implementations
    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return unwrapType.isAssignableFrom(getClass());
    }

    // Returns the current instance when Hibernate requests a native type
    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return (T) this;
    }

    // Provides a generic connection—used when no tenant is specified
    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Releases a connection not bound to any tenant
    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    /**
     * Provides a tenant-specific connection by setting the schema dynamically.
     * - Prevents SQL injection by stripping quotes from tenant identifier
     * - Uses PostgreSQL's 'SET search_path' to switch schemas
     */
    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        final Connection connection = getAnyConnection();
        String schema = tenantIdentifier.toString().replace("\"", ""); // Sanitize input
        connection.createStatement().execute("SET search_path TO \"" + schema + "\"");
        return connection;
    }

    /**
     * Cleans up after tenant-specific usage.
     * - Resets schema to 'public' to avoid leaking tenant context
     * - Closes connection afterward
     */
    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        connection.createStatement().execute("SET search_path TO public");
        connection.close();
    }

    // Indicates that connections should not be aggressively released between transactions
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }
}