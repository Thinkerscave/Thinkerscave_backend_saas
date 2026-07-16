package com.thinkerscave.config.tenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PostgreSQL schema switching via {@code SET search_path}.
 * Not active unless {@code app.tenancy.schema-switcher=postgresql}.
 * Prepared for future Hostinger / PostgreSQL production deployment.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.tenancy.schema-switcher", havingValue = "postgresql")
public class PostgresSearchPathSchemaSwitcher implements TenantSchemaSwitcher {

    private final String platformSchema;

    public PostgresSearchPathSchemaSwitcher(
            @Value("${app.tenancy.platform-schema:public}") String platformSchema) {
        this.platformSchema = platformSchema;
    }

    @Override
    public void switchToTenant(Connection connection, String tenantIdentifier) throws SQLException {
        String schema = resolvePhysicalSchema(tenantIdentifier);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + quoteIdentifier(schema));
        }
    }

    @Override
    public void resetToPlatform(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + quoteIdentifier(platformSchema));
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

    /** Basic identifier quoting to avoid SQL injection on schema names. */
    private String quoteIdentifier(String identifier) {
        if (!identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid schema identifier");
        }
        return "\"" + identifier + "\"";
    }
}
