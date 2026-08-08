package com.thinkerscave.config;

import com.thinkerscave.config.tenancy.TenantSchemaSwitcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Hibernate multi-tenancy connection provider.
 * Delegates physical schema/catalog switching to {@link TenantSchemaSwitcher}
 * (MySQL catalog today; PostgreSQL search_path when configured).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;
    private final TenantSchemaSwitcher schemaSwitcher;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        resetAndClose(connection);
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            schemaSwitcher.switchToTenant(connection, tenantIdentifier);
            return connection;
        } catch (SQLException ex) {
            log.error("Cannot switch to tenant='{}' (schema='{}'): {}",
                    tenantIdentifier, schemaSwitcher.resolvePhysicalSchema(tenantIdentifier), ex.getMessage());
            try {
                connection.close();
            } catch (SQLException closeEx) {
                ex.addSuppressed(closeEx);
            }
            // Fail closed — never fall back to the platform catalog for tenant work.
            throw ex;
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        resetAndClose(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType)
                || DataSource.class.isAssignableFrom(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new org.hibernate.HibernateException(
                "Cannot unwrap [" + unwrapType.getName() + "] from " + getClass().getName());
    }

    private void resetAndClose(Connection connection) throws SQLException {
        try {
            schemaSwitcher.resetToPlatform(connection);
        } catch (SQLException ex) {
            log.debug("Could not reset schema before releasing connection: {}", ex.getMessage());
        } finally {
            connection.close();
        }
    }
}
