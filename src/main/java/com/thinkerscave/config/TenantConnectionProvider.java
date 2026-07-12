package com.thinkerscave.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Hibernate multi-tenancy connection provider.
 *
 * <p>Routes each database request to the correct MySQL catalog (schema) based on the
 * current tenant identifier resolved by {@link TenantIdentifierResolver}.
 *
 * <p>Schema naming convention:
 * <ul>
 *   <li>{@code "public"} or {@code "platform"} → {@value #PLATFORM_SCHEMA} (admin / super-admin)</li>
 *   <li>{@code "jsb-bhubaneswar"} or {@code "jsb_bhubaneswar"} → {@code tenant_jsb_bhubaneswar}</li>
 *   <li>Any other tenant slug → {@code tenant_<slug_with_underscores>}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    /** The platform / admin schema — always routed for "public" or "platform" tenants. */
    static final String PLATFORM_SCHEMA = "thinkerscave_dev";

    private final DataSource dataSource;

    // -------------------------------------------------------------------------
    // MultiTenantConnectionProvider contract
    // -------------------------------------------------------------------------

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
        String schema = resolveSchema(tenantIdentifier);
        Connection connection = dataSource.getConnection();
        try {
            if (!schema.equals(connection.getCatalog())) {
                connection.setCatalog(schema);
            }
        } catch (SQLException ex) {
            log.warn("Cannot switch to schema '{}' (tenant='{}'): {}. Using default.",
                    schema, tenantIdentifier, ex.getMessage());
            // Fall through — connection stays on the default catalog
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        resetAndClose(connection);
    }

    /**
     * Returns {@code false} because each HTTP request may use a different tenant,
     * so aggressive connection release (re-use across logical sessions) is unsafe.
     */
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    // Required by org.hibernate.service.spi.Wrapped
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
        throw new org.hibernate.HibernateException("Cannot unwrap [" + unwrapType.getName() + "] from " + getClass().getName());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resets the connection to the platform schema before returning it to the pool
     * so the next borrower gets a clean default catalog.
     */
    private void resetAndClose(Connection connection) throws SQLException {
        try {
            if (!PLATFORM_SCHEMA.equals(connection.getCatalog())) {
                connection.setCatalog(PLATFORM_SCHEMA);
            }
        } catch (SQLException ex) {
            log.debug("Could not reset catalog before releasing connection: {}", ex.getMessage());
        } finally {
            connection.close();
        }
    }

    /**
     * Maps a tenant slug to its MySQL database (catalog) name.
     *
     * @param tenantIdentifier the raw slug from {@link TenantIdentifierResolver}
     * @return the MySQL catalog name to use
     */
    static String resolveSchema(String tenantIdentifier) {
        if (tenantIdentifier == null || tenantIdentifier.isBlank()
                || "public".equalsIgnoreCase(tenantIdentifier)
                || "platform".equalsIgnoreCase(tenantIdentifier)) {
            return PLATFORM_SCHEMA;
        }
        // Normalize: jsb-bhubaneswar → jsb_bhubaneswar, then prefix with "tenant_"
        String normalized = tenantIdentifier.toLowerCase().replace('-', '_');
        // Already has prefix (safety guard)
        if (normalized.startsWith("tenant_")) {
            return normalized;
        }
        return "tenant_" + normalized;
    }
}
