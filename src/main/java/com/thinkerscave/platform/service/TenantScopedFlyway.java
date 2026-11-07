package com.thinkerscave.platform.service;

import com.thinkerscave.platform.entity.TenantRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantScopedFlyway {
    public static final String HISTORY_TABLE = "flyway_tenant_schema_history";
    public static final String TENANT_SCRIPT_LOCATION = "db/migration/tenant";
    public static final String BASELINE_VERSION = "1";
    /**
     * Pinned default target for provisioning and "migrate to latest" actions.
     * V55 (retire alumni/transfer menus) and V56 (drop unused tenant metadata tables) were
     * approved by the owner on 2026-11-07; {@link #verifyPackagedTargetConsistency()} logs drift.
     */
    public static final String LATEST_VERSION = "56";
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

    /**
     * Read-only Flyway inventory for a tenant schema. Never executes migrations or writes history.
     */
    public TenantInfo info(TenantRegistry tenant) {
        String schema = trustedSchema(tenant);
        if (!inspect(schema).hasHistory()) {
            return new TenantInfo(schema, null, List.of(), List.of(), 0, 0);
        }
        Flyway flyway = configured(schema, null, false);
        MigrationInfo[] all = flyway.info().all();
        MigrationInfo current = flyway.info().current();
        List<ScriptState> applied = new ArrayList<>();
        List<ScriptState> pending = new ArrayList<>();
        for (MigrationInfo migration : all) {
            if (migration.getVersion() == null) {
                continue;
            }
            ScriptState state = toScriptState(migration);
            if (migration.getState() != null && migration.getState().isApplied()) {
                applied.add(state);
            } else if (migration.getState() == MigrationState.PENDING
                    || migration.getState() == MigrationState.OUT_OF_ORDER
                    || migration.getState() == MigrationState.ABOVE_TARGET) {
                pending.add(state);
            }
        }
        applied.sort(Comparator.comparing(ScriptState::version, VERSION_ORDER));
        pending.sort(Comparator.comparing(ScriptState::version, VERSION_ORDER));
        String currentVersion = current != null && current.getVersion() != null
                ? current.getVersion().getVersion() : null;
        return new TenantInfo(schema, currentVersion, applied, pending,
                applied.size(), pending.size());
    }

    /**
     * Read-only Flyway validation. Returns the outcome instead of throwing so callers can surface it.
     */
    public ValidationOutcome validate(TenantRegistry tenant) {
        String schema = trustedSchema(tenant);
        if (!inspect(schema).hasHistory()) {
            return new ValidationOutcome(false,
                    "Tenant schema has no isolated Flyway history; the first migration will baseline it.");
        }
        try {
            ValidateResult result = configured(schema, null, false).validateWithResult();
            if (result.validationSuccessful) {
                return new ValidationOutcome(true, "All applied tenant migrations match the packaged scripts.");
            }
            List<String> errors = new ArrayList<>();
            for (var invalid : result.invalidMigrations) {
                errors.add(invalid.version + " " + invalid.description + ": "
                        + (invalid.errorDetails == null ? "validation failed" : invalid.errorDetails.errorMessage));
            }
            if (result.errorDetails != null && StringUtils.hasText(result.errorDetails.errorMessage)) {
                errors.add(result.errorDetails.errorMessage);
            }
            return new ValidationOutcome(false, errors.isEmpty()
                    ? "Flyway validation failed" : String.join(" | ", errors));
        } catch (Exception ex) {
            return new ValidationOutcome(false, sanitize(ex));
        }
    }

    /**
     * Discovers packaged Flyway scripts for the given classpath location (read-only, no database access).
     */
    public List<DiscoveredScript> discover(String location) {
        List<DiscoveredScript> scripts = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:" + location + "/*.sql");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                Matcher matcher = SCRIPT_PATTERN.matcher(filename);
                if (!matcher.matches()) {
                    continue;
                }
                scripts.add(new DiscoveredScript(matcher.group(1), matcher.group(2).replace('_', ' '),
                        filename, location));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to discover Flyway scripts in " + location, ex);
        }
        scripts.sort(Comparator.comparing(DiscoveredScript::version, VERSION_ORDER));
        return scripts;
    }

    /**
     * Highest tenant migration version packaged in the application. Read-only, no database access.
     */
    public String latestPackagedVersion() {
        List<DiscoveredScript> scripts = discover(TENANT_SCRIPT_LOCATION);
        if (scripts.isEmpty()) {
            throw new IllegalStateException("No tenant Flyway scripts are packaged");
        }
        return scripts.get(scripts.size() - 1).version();
    }

    /**
     * Surfaces drift between the pinned provisioning target and the packaged scripts.
     * Never changes migration behaviour; it only warns so the constant cannot silently go stale.
     */
    @PostConstruct
    void verifyPackagedTargetConsistency() {
        try {
            String packaged = latestPackagedVersion();
            if (!packaged.equals(LATEST_VERSION)) {
                log.warn("Tenant migration target is pinned to V{} but packaged scripts reach V{}. "
                                + "Update TenantScopedFlyway.LATEST_VERSION only after the newer scripts "
                                + "are approved for execution.",
                        LATEST_VERSION, packaged);
            }
        } catch (Exception ex) {
            log.warn("Unable to verify packaged tenant migration versions: {}", ex.getMessage());
        }
    }

    private ScriptState toScriptState(MigrationInfo migration) {
        LocalDateTime installedOn = migration.getInstalledOn() == null ? null
                : LocalDateTime.ofInstant(migration.getInstalledOn().toInstant(), ZoneId.systemDefault());
        return new ScriptState(migration.getVersion().getVersion(), migration.getDescription(),
                migration.getScript(), migration.getState() == null ? null : migration.getState().name(),
                migration.getChecksum(), installedOn, migration.getExecutionTime());
    }

    private String sanitize(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
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
                // V49 restores V22's schema for upgraded tenants without replaying its catalog changes.
                // Pending scripts are expected before a migration and must not fail validation.
                .ignoreMigrationPatterns("*:missing", "*:ignored", "*:pending")
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

    public record ScriptState(String version, String description, String script, String state,
                              Integer checksum, LocalDateTime installedOn, Integer executionTimeMs) {}

    public record TenantInfo(String schemaName, String currentVersion, List<ScriptState> applied,
                             List<ScriptState> pending, int appliedCount, int pendingCount) {}

    public record ValidationOutcome(boolean valid, String message) {}

    public record DiscoveredScript(String version, String description, String script, String location) {}

    private static final Pattern SCRIPT_PATTERN = Pattern.compile("^V([0-9][0-9._]*)__(.+)\\.sql$");
    private static final Comparator<String> VERSION_ORDER = (left, right) -> {
        try {
            return MigrationVersion.fromVersion(left).compareTo(MigrationVersion.fromVersion(right));
        } catch (Exception ex) {
            return left.compareTo(right);
        }
    };
}
