package com.thinkerscave.common.config;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Dev-mode MultiTenantConnectionProvider that does NOT switch schemas.
 * H2 doesn't support PostgreSQL's SET search_path, so in dev mode
 * all data lives in the default PUBLIC schema.
 * 
 * This preserves the multi-tenant architecture contracts while
 * simplifying local development.
 */
@Component
@Profile("dev")
public class DevMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    public DevMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        // In dev mode, just return a plain connection (no schema switching)
        return dataSource.getConnection();
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return unwrapType.isAssignableFrom(getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        return (T) this;
    }
}
