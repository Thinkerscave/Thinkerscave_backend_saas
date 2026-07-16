package com.thinkerscave.config.tenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * MySQL catalog (database) switching — used by the current development stack.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.tenancy.schema-switcher", havingValue = "mysql", matchIfMissing = true)
public class MysqlCatalogSchemaSwitcher implements TenantSchemaSwitcher {

    private final String platformSchema;

    public MysqlCatalogSchemaSwitcher(
            @Value("${app.tenancy.platform-schema:thinkerscave_dev}") String platformSchema) {
        this.platformSchema = platformSchema;
    }

    @Override
    public void switchToTenant(Connection connection, String tenantIdentifier) throws SQLException {
        String schema = resolvePhysicalSchema(tenantIdentifier);
        if (!schema.equals(connection.getCatalog())) {
            connection.setCatalog(schema);
        }
    }

    @Override
    public void resetToPlatform(Connection connection) throws SQLException {
        if (!platformSchema.equals(connection.getCatalog())) {
            connection.setCatalog(platformSchema);
        }
    }

    @Override
    public String resolvePhysicalSchema(String tenantIdentifier) {
        if (!StringUtils.hasText(tenantIdentifier)
                || "public".equalsIgnoreCase(tenantIdentifier)
                || "platform".equalsIgnoreCase(tenantIdentifier)) {
            return platformSchema;
        }
        String normalized = tenantIdentifier.toLowerCase().replace('-', '_');
        if (normalized.startsWith("tenant_")) {
            return normalized;
        }
        return "tenant_" + normalized;
    }
}
