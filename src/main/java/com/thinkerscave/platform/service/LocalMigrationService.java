package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.response.LocalMigrationRunResponse;
import com.thinkerscave.platform.dto.response.LocalMigrationStatusResponse;
import com.thinkerscave.platform.dto.response.ScriptStateResponse;
import com.thinkerscave.shared.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Guarded local-development Flyway workflow.
 *
 * <p>This is deliberately defensive: it is disabled by default, refuses any non-loopback
 * datasource, never accepts a caller-supplied datasource or SQL, and prevents concurrent runs.
 * It can therefore never be pointed at the shared development/TEST/PROD database that the
 * platform currently uses.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalMigrationService {
    private static final List<String> LOOPBACK_HOSTS = List.of("localhost", "127.0.0.1", "::1", "[::1]");

    private final DataSource dataSource;
    private final ObjectProvider<Flyway> flywayProvider;
    private final Environment environment;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${app.migration.local.enabled:false}")
    private boolean enabled;

    public LocalMigrationStatusResponse status() {
        DataSourceInfo info = dataSourceInfo();
        Flyway flyway = flywayProvider.getIfAvailable();
        List<String> blockers = new ArrayList<>();
        if (!enabled) {
            blockers.add("Local migration workflow is disabled. Set APP_MIGRATION_LOCAL_ENABLED=true "
                    + "for a verified isolated local environment.");
        }
        if (!info.loopback()) {
            blockers.add("Datasource host '" + info.host() + "' is not loopback. The local workflow "
                    + "refuses to run against shared or remote databases.");
        }
        if (flyway == null) {
            blockers.add("Flyway is not available for the active datasource.");
        }

        String current = null;
        String latest = null;
        List<ScriptStateResponse> pending = new ArrayList<>();
        boolean validationSuccessful = false;
        String validationMessage = "Validation not attempted";
        if (flyway != null) {
            try {
                MigrationInfo currentInfo = flyway.info().current();
                current = currentInfo != null && currentInfo.getVersion() != null
                        ? currentInfo.getVersion().getVersion() : null;
                for (MigrationInfo migration : flyway.info().all()) {
                    if (migration.getVersion() == null) {
                        continue;
                    }
                    latest = migration.getVersion().getVersion();
                    if (migration.getState() == MigrationState.PENDING
                            || migration.getState() == MigrationState.OUT_OF_ORDER) {
                        pending.add(toScriptState(migration));
                    }
                }
                ValidateResult result = flyway.validateWithResult();
                validationSuccessful = result.validationSuccessful;
                validationMessage = result.validationSuccessful
                        ? "Packaged scripts match the applied migration history."
                        : result.getAllErrorMessages();
            } catch (Exception ex) {
                validationMessage = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            }
        }

        return new LocalMigrationStatusResponse(enabled, info.loopback(), info.host(), info.database(),
                String.join(",", environment.getActiveProfiles()), flyway != null,
                current, latest, pending.size(), pending, validationSuccessful, validationMessage,
                blockers, LocalDateTime.now());
    }

    public LocalMigrationRunResponse validate() {
        Flyway flyway = requireFlyway();
        try {
            ValidateResult result = flyway.validateWithResult();
            return new LocalMigrationRunResponse(false,
                    result.validationSuccessful ? "VALID" : "INVALID",
                    result.validationSuccessful
                            ? "Packaged local scripts match the applied migration history."
                            : result.getAllErrorMessages(),
                    null, null, 0, List.of(), LocalDateTime.now());
        } catch (Exception ex) {
            return new LocalMigrationRunResponse(false, "INVALID", message(ex),
                    null, null, 0, List.of(), LocalDateTime.now());
        }
    }

    public LocalMigrationRunResponse migrate() {
        Flyway flyway = requireFlyway();
        if (!running.compareAndSet(false, true)) {
            throw new BadRequestException("A local migration run is already in progress");
        }
        try {
            MigrationInfo before = flyway.info().current();
            String beforeVersion = before != null && before.getVersion() != null
                    ? before.getVersion().getVersion() : null;
            MigrateResult result = flyway.migrate();
            MigrationInfo after = flyway.info().current();
            String afterVersion = after != null && after.getVersion() != null
                    ? after.getVersion().getVersion() : null;
            List<ScriptStateResponse> applied = new ArrayList<>();
            for (MigrationInfo migration : flyway.info().all()) {
                if (migration.getVersion() == null) {
                    continue;
                }
                if (migration.getState() == MigrationState.SUCCESS) {
                    applied.add(toScriptState(migration));
                }
            }
            log.info("Local Flyway run completed before={} after={} executed={}",
                    beforeVersion, afterVersion, result.migrationsExecuted);
            return new LocalMigrationRunResponse(true,
                    result.migrationsExecuted == 0 ? "UP_TO_DATE" : "EXECUTED",
                    result.migrationsExecuted == 0
                            ? "Local database was already at the latest packaged version."
                            : result.migrationsExecuted + " local migration(s) applied.",
                    beforeVersion, afterVersion, result.migrationsExecuted, applied, LocalDateTime.now());
        } catch (Exception ex) {
            log.error("Local Flyway run failed", ex);
            return new LocalMigrationRunResponse(false, "FAILED", message(ex),
                    null, null, 0, List.of(), LocalDateTime.now());
        } finally {
            running.set(false);
        }
    }

    private Flyway requireFlyway() {
        DataSourceInfo info = dataSourceInfo();
        List<String> blockers = new ArrayList<>();
        if (!enabled) {
            blockers.add("Local migration workflow is disabled (app.migration.local.enabled=false).");
        }
        if (!info.loopback()) {
            blockers.add("Datasource host '" + info.host() + "' is not loopback; refusing to run.");
        }
        Flyway flyway = flywayProvider.getIfAvailable();
        if (flyway == null) {
            blockers.add("Flyway is not available for the active datasource.");
        }
        if (!blockers.isEmpty()) {
            throw new BadRequestException(String.join(" ", blockers));
        }
        return flyway;
    }

    private DataSourceInfo dataSourceInfo() {
        String url = null;
        try (Connection connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        } catch (Exception ex) {
            log.debug("Unable to read datasource metadata", ex);
        }
        String host = null;
        String database = null;
        if (url != null && url.startsWith("jdbc:")) {
            String remainder = url.substring(url.indexOf("//") + 2);
            int slash = remainder.indexOf('/');
            String authority = slash < 0 ? remainder : remainder.substring(0, slash);
            int query = authority.indexOf('?');
            if (query >= 0) {
                authority = authority.substring(0, query);
            }
            int port = authority.lastIndexOf(':');
            host = port > 0 && authority.indexOf(':') == port ? authority.substring(0, port) : authority;
            if (slash >= 0) {
                database = remainder.substring(slash + 1);
                int queryIndex = database.indexOf('?');
                if (queryIndex >= 0) {
                    database = database.substring(0, queryIndex);
                }
            }
        }
        boolean loopback = host != null && LOOPBACK_HOSTS.contains(host.toLowerCase());
        return new DataSourceInfo(host, database, loopback);
    }

    private ScriptStateResponse toScriptState(MigrationInfo migration) {
        LocalDateTime installedOn = migration.getInstalledOn() == null ? null
                : LocalDateTime.ofInstant(migration.getInstalledOn().toInstant(), ZoneId.systemDefault());
        return new ScriptStateResponse(migration.getVersion().getVersion(), migration.getDescription(),
                migration.getScript(), migration.getState() == null ? null : migration.getState().name(),
                migration.getChecksum(), installedOn, migration.getExecutionTime());
    }

    private String message(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private record DataSourceInfo(String host, String database, boolean loopback) {
    }
}
