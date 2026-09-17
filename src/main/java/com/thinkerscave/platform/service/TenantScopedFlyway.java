package com.thinkerscave.platform.service;

import com.thinkerscave.platform.entity.TenantRegistry;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TenantScopedFlyway {
    public static final String HISTORY_TABLE = "flyway_tenant_schema_history";
    public static final String BASELINE_VERSION = "1";
    public static final String LATEST_VERSION = "5";
    private static final List<String> FINGERPRINT = List.of(
            "users", "roles", "user_roles", "tenant_registry", "organizations");

    private final DataSource dataSource;

    public MigrationRun migrate(TenantRegistry tenant, String targetVersion) {
        String schema = trustedSchema(tenant);
        if (!StringUtils.hasText(targetVersion)) {
            throw new IllegalArgumentException("A release target version is required");
        }
        validateTargetCap(schema, targetVersion);

        SchemaState state = inspect(schema);
        if (state.nonEmpty() && !state.hasHistory()) {
            validateCanonicalFingerprint(schema);
            baseline(schema);
        }

        Flyway flyway = configured(schema, targetVersion, false);
        String before = currentVersion(flyway);
        MigrationVersion requested = MigrationVersion.fromVersion(targetVersion);
        if (before != null && MigrationVersion.fromVersion(before).compareTo(requested) > 0) {
            throw new IllegalArgumentException("Tenant database downgrades are not supported");
        }
        MigrateResult result = flyway.migrate();
        String after = currentVersion(flyway);
        if (after == null || MigrationVersion.fromVersion(after).compareTo(requested) != 0) {
            throw new IllegalStateException("Tenant database did not reach target version " + targetVersion);
        }
        return new MigrationRun(before, after, result.migrationsExecuted, List.of(flyway.info().all()));
    }

    public String currentVersion(TenantRegistry tenant) {
        String schema = trustedSchema(tenant);
        if (!inspect(schema).hasHistory()) {
            return null;
        }
        return currentVersion(configured(schema, null, false));
    }

    private Flyway configured(String schema, String target, boolean baselineOnMigrate) {
        var configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tenant")
                .schemas(schema)
                .defaultSchema(schema)
                .table(HISTORY_TABLE)
                .baselineOnMigrate(baselineOnMigrate)
                .validateMigrationNaming(true)
                .outOfOrder(false)
                .cleanDisabled(true);
        if (StringUtils.hasText(target)) {
            configuration.target(MigrationVersion.fromVersion(target));
        }
        return configuration.load();
    }

    private void baseline(String schema) {
        configured(schema, null, false).getConfiguration();
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tenant")
                .schemas(schema)
                .defaultSchema(schema)
                .table(HISTORY_TABLE)
                .baselineVersion(MigrationVersion.fromVersion(BASELINE_VERSION))
                .baselineDescription("Validated canonical tenant schema")
                .cleanDisabled(true)
                .load()
                .baseline();
    }

    private String currentVersion(Flyway flyway) {
        MigrationInfo current = flyway.info().current();
        return current != null && current.getVersion() != null ? current.getVersion().getVersion() : null;
    }

    private void validateTargetCap(String schema, String target) {
        MigrationVersion requested = MigrationVersion.fromVersion(target);
        MigrationVersion highest = null;
        boolean targetExists = false;
        for (MigrationInfo migration : configured(schema, null, false).info().all()) {
            if (migration.getVersion() != null
                    && (highest == null || migration.getVersion().compareTo(highest) > 0)) {
                highest = migration.getVersion();
            }
            if (migration.getVersion() != null && migration.getVersion().compareTo(requested) == 0) {
                targetExists = true;
            }
        }
        if (highest == null || requested.compareTo(highest) > 0) {
            throw new IllegalArgumentException("Target database version exceeds available tenant migrations");
        }
        if (!targetExists) {
            throw new IllegalArgumentException("Target database version does not exist");
        }
    }

    private SchemaState inspect(String schema) {
        String sql = """
                SELECT COUNT(*) AS table_count,
                       COUNT(*) FILTER (WHERE table_name = ?) AS history_count
                FROM information_schema.tables
                WHERE table_schema = ? AND table_type = 'BASE TABLE'
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, HISTORY_TABLE);
            statement.setString(2, schema);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return new SchemaState(result.getLong("table_count") > 0,
                        result.getLong("history_count") == 1);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to inspect tenant schema", ex);
        }
    }

    private void validateCanonicalFingerprint(String schema) {
        String sql = """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ANY (?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setArray(2, connection.createArrayOf("varchar", FINGERPRINT.toArray()));
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                if (result.getInt(1) != FINGERPRINT.size()) {
                    throw new IllegalStateException(
                            "Existing schema failed canonical V1 fingerprint; baseline refused");
                }
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to validate tenant schema fingerprint", ex);
        }
    }

    private String trustedSchema(TenantRegistry tenant) {
        if (tenant == null || tenant.getId() == null || !StringUtils.hasText(tenant.getSchemaName())
                || !tenant.getSchemaName().matches("tenant_[a-z0-9_]+")) {
            throw new IllegalArgumentException("Tenant registry does not contain a trusted tenant schema");
        }
        return tenant.getSchemaName();
    }

    private record SchemaState(boolean nonEmpty, boolean hasHistory) {}

    public record MigrationRun(String beforeVersion, String afterVersion, int migrationsExecuted,
                               List<MigrationInfo> migrationInfo) {}
}
