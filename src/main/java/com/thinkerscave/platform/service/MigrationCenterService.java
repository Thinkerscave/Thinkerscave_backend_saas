package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.CreateReleaseRequest;
import com.thinkerscave.platform.dto.request.MaintenanceModeRequest;
import com.thinkerscave.platform.dto.response.AuditEventResponse;
import com.thinkerscave.platform.dto.response.CatalogOverviewResponse;
import com.thinkerscave.platform.dto.response.CatalogSyncExecutionResponse;
import com.thinkerscave.platform.dto.response.CatalogSyncStatusResponse;
import com.thinkerscave.platform.dto.response.CatalogVersionResponse;
import com.thinkerscave.platform.dto.response.ExecutionJobResponse;
import com.thinkerscave.platform.dto.response.MigrationCenterOverviewResponse;
import com.thinkerscave.platform.dto.response.MigrationExecutionResponse;
import com.thinkerscave.platform.dto.response.MigrationPreviewResponse;
import com.thinkerscave.platform.dto.response.MigrationScriptResponse;
import com.thinkerscave.platform.dto.response.ReleaseDetailResponse;
import com.thinkerscave.platform.dto.response.ReleaseOverviewResponse;
import com.thinkerscave.platform.dto.response.ReleaseValidationResponse;
import com.thinkerscave.platform.dto.response.ScriptStateResponse;
import com.thinkerscave.platform.dto.response.TenantCatalogRowResponse;
import com.thinkerscave.platform.dto.response.TenantMigrationDetailResponse;
import com.thinkerscave.platform.dto.response.TenantMigrationResponse;
import com.thinkerscave.platform.dto.response.TenantReleaseDetailResponse;
import com.thinkerscave.platform.dto.response.TenantReleaseOperationResponse;
import com.thinkerscave.platform.dto.response.TenantHealthResponse;
import com.thinkerscave.platform.dto.response.ValidationCheckResponse;
import com.thinkerscave.platform.entity.PlatformRelease;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.OperationStatus;
import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.platform.enums.ReleaseStatus;
import com.thinkerscave.platform.repository.PlatformReleaseRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Read-mostly orchestration layer behind the Migration Center UI.
 *
 * <p>Three independent lifecycles are surfaced here: application releases, Flyway database
 * migrations and catalog synchronization. Nothing in this service deploys an application build,
 * edits an applied versioned migration or manipulates Flyway history directly. Execution methods
 * delegate to the existing tenant migration / catalog sync services which already enforce locking,
 * maintenance mode, auditing and retry semantics.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationCenterService {
    static final String TENANT_LOCATION = "db/migration/tenant";
    private static final Pattern TENANT_SCHEMA = Pattern.compile("tenant_[a-z0-9_]+");
    private static final String JOB_UNION = """
            SELECT 'MIGRATION' AS job_type, e.id AS job_id, t.id AS tenant_id, t.tenant_identifier,
                   o.organization_name, r.release_id AS release_ref, e.status, e.attempt,
                   e.target_version, e.started_at, e.completed_at, e.error_message,
                   COALESCE(e.started_at, e.created_on) AS activity_at
            FROM tenant_migration_executions e
            JOIN tenant_registry t ON t.id = e.tenant_registry_id
            JOIN organizations o ON o.id = t.organization_id
            LEFT JOIN platform_releases r ON r.id = e.release_id
            UNION ALL
            SELECT 'CATALOG_SYNC', c.id, t.id, t.tenant_identifier, o.organization_name, NULL, c.status, c.attempt,
                   cv.version_number::text, c.started_at, c.completed_at, c.error_message,
                   COALESCE(c.started_at, c.created_on)
            FROM catalog_sync_executions c
            JOIN tenant_registry t ON t.id = c.tenant_registry_id
            JOIN organizations o ON o.id = t.organization_id
            JOIN catalog_versions cv ON cv.id = c.catalog_version_id
            UNION ALL
            SELECT 'PROVISIONING', p.id, tr.id, COALESCE(tr.tenant_identifier, ''), o.organization_name, NULL,
                   p.status, COALESCE(p.retry_count, 0) + 1, NULL, p.started_at, p.completed_at, p.error_message,
                   COALESCE(p.started_at, p.created_on)
            FROM provisioning_jobs p
            JOIN organizations o ON o.id = p.organization_id
            LEFT JOIN tenant_registry tr ON tr.id = p.tenant_registry_id
            """;

    private final TenantRegistryRepository tenantRepository;
    private final PlatformReleaseRepository releaseRepository;
    private final TenantMigrationService migrationService;
    private final ReleaseService releaseService;
    private final TenantReleaseOperationsService operationsService;
    private final TenantCatalogSyncService catalogSyncService;
    private final TenantHealthService healthService;
    private final TenantScopedFlyway tenantFlyway;
    private final JdbcTemplate jdbcTemplate;

    /* ------------------------------------------------------------------ overview */

    @Transactional(readOnly = true)
    public MigrationCenterOverviewResponse overview() {
        List<TenantRegistry> tenants = eligibleTenants();
        PlatformRelease release = releaseRepository.findTopByOrderByCreatedOnDesc().orElse(null);
        String targetDatabase = release == null ? null : release.getTargetDatabaseVersion();
        String targetCatalog = release == null ? null : release.getTargetCatalogVersion();
        Map<Long, String> latestMigration = latestStatus("tenant_migration_executions");
        Map<Long, String> latestCatalog = latestStatus("catalog_sync_executions");

        long databaseCurrent = 0;
        long databasePending = 0;
        long databaseFailed = 0;
        long catalogSynced = 0;
        long catalogPending = 0;
        long catalogFailed = 0;
        long maintenance = 0;

        for (TenantRegistry tenant : tenants) {
            String currentVersion = safeCurrentVersion(tenant);
            String migrationStatus = latestMigration.get(tenant.getId());
            if (OperationStatus.FAILED.name().equals(migrationStatus)) {
                databaseFailed++;
            } else if (targetDatabase != null && targetDatabase.equals(currentVersion)) {
                databaseCurrent++;
            } else {
                databasePending++;
            }

            String catalogVersion = tenant.getCatalogVersion() == null
                    ? null : String.valueOf(tenant.getCatalogVersion());
            String catalogStatus = latestCatalog.get(tenant.getId());
            if (OperationStatus.FAILED.name().equals(catalogStatus)) {
                catalogFailed++;
            } else if (targetCatalog != null && targetCatalog.equals(catalogVersion)) {
                catalogSynced++;
            } else {
                catalogPending++;
            }

            if (Boolean.TRUE.equals(tenant.getMaintenanceMode())) {
                maintenance++;
            }
        }

        return new MigrationCenterOverviewResponse(
                latestAvailableVersion(),
                latestCatalogVersion(),
                release == null ? null : release.getId(),
                release == null ? null : release.getReleaseVersion(),
                release == null ? null : release.getStatus().name(),
                release == null ? null : release.getApplicationVersion(),
                targetDatabase,
                targetCatalog,
                tenants.size(),
                databaseCurrent,
                databasePending,
                databaseFailed,
                catalogSynced,
                catalogPending,
                catalogFailed,
                maintenance,
                countExecutions("tenant_migration_executions", "status IN ('RUNNING','PENDING')")
                        + countExecutions("catalog_sync_executions", "status IN ('RUNNING','PENDING')"),
                countExecutions("tenant_migration_executions", "status = 'FAILED'")
                        + countExecutions("catalog_sync_executions", "status = 'FAILED'"),
                releaseRepository.count(),
                tenantFlyway.discover(TENANT_LOCATION).size(),
                LocalDateTime.now());
    }

    /* ------------------------------------------------------------------ releases */

    @Transactional(readOnly = true)
    public Page<ReleaseOverviewResponse> releases(Pageable pageable) {
        Map<Long, Map<String, Long>> counts = executionCountsByRelease();
        return releaseRepository.findAll(pageable)
                .map(release -> toOverview(release, counts.getOrDefault(release.getId(), Map.of())));
    }

    @Transactional(readOnly = true)
    public ReleaseDetailResponse releaseDetail(Long id) {
        PlatformRelease release = findRelease(id);
        ReleaseOverviewResponse overview = toOverview(release, executionCounts(release.getId()));
        return new ReleaseDetailResponse(overview, validateRelease(id), jobsForRelease(id),
                auditForEntity("RELEASE", String.valueOf(id), 100), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public ReleaseValidationResponse validateRelease(Long id) {
        PlatformRelease release = findRelease(id);
        String latestDatabase = latestAvailableVersion();
        String currentCatalog = latestCatalogVersion();
        long eligible = eligibleTenants().size();

        List<ValidationCheckResponse> checks = new ArrayList<>();
        boolean databaseScriptExists = packagedVersions().contains(release.getTargetDatabaseVersion());
        checks.add(new ValidationCheckResponse("database_target", "Database target script exists",
                databaseScriptExists ? "PASS" : "FAIL",
                databaseScriptExists
                        ? "Flyway script V" + release.getTargetDatabaseVersion() + " is packaged"
                        : "No packaged tenant script matches V" + release.getTargetDatabaseVersion()));

        boolean withinRange = latestDatabase != null
                && compareVersions(release.getTargetDatabaseVersion(), latestDatabase) <= 0;
        checks.add(new ValidationCheckResponse("database_range", "Target within packaged range",
                withinRange ? "PASS" : "FAIL",
                withinRange ? "Highest packaged version is V" + latestDatabase
                        : "Target exceeds the highest packaged tenant script"));

        boolean catalogMatches = currentCatalog != null && currentCatalog.equals(release.getTargetCatalogVersion());
        checks.add(new ValidationCheckResponse("catalog_target", "Catalog target equals published catalog",
                catalogMatches ? "PASS" : "FAIL",
                catalogMatches ? "Published catalog version is " + currentCatalog
                        : "Published catalog version is " + currentCatalog + "; release targets "
                        + release.getTargetCatalogVersion()));

        boolean hasTenants = eligible > 0;
        checks.add(new ValidationCheckResponse("eligible_tenants", "Provisioned tenants available",
                hasTenants ? "PASS" : "FAIL",
                hasTenants ? eligible + " provisioned tenant(s) can receive this release"
                        : "No active, provisioned tenants are registered"));

        boolean notAlreadyExecuting = release.getStatus() != ReleaseStatus.READY;
        checks.add(new ValidationCheckResponse("release_state", "Release not currently executing",
                notAlreadyExecuting ? "PASS" : "FAIL",
                notAlreadyExecuting ? "Release status is " + release.getStatus()
                        : "Release is claimed by a running execution"));

        boolean notesPresent = StringUtils.hasText(release.getReleaseNotes());
        checks.add(new ValidationCheckResponse("release_notes", "Release notes captured",
                notesPresent ? "PASS" : "WARN",
                notesPresent ? "Operational notes are recorded"
                        : "No release notes recorded; operators lose change context"));

        boolean valid = checks.stream().noneMatch(check -> "FAIL".equals(check.status()));
        return new ReleaseValidationResponse(valid, release.getReleaseId(),
                release.getTargetDatabaseVersion(), latestDatabase,
                release.getTargetCatalogVersion(), currentCatalog, eligible, checks, LocalDateTime.now());
    }

    public ReleaseOverviewResponse createRelease(CreateReleaseRequest request) {
        var created = releaseService.create(request);
        PlatformRelease release = findRelease(created.id());
        return toOverview(release, Map.of());
    }

    public ReleaseOverviewResponse executeRelease(Long id) {
        PlatformRelease release = findRelease(id);
        String latestDatabase = latestAvailableVersion();
        if (latestDatabase != null && compareVersions(release.getTargetDatabaseVersion(), latestDatabase) > 0) {
            throw new BadRequestException("Release database target V" + release.getTargetDatabaseVersion()
                    + " exceeds the highest packaged tenant script V" + latestDatabase
                    + "; deploy the build containing that script first");
        }
        if (!packagedVersions().contains(release.getTargetDatabaseVersion())) {
            throw new BadRequestException("No packaged tenant Flyway script matches release target V"
                    + release.getTargetDatabaseVersion());
        }
        var executed = releaseService.execute(id);
        return toOverview(findRelease(executed.id()), executionCounts(id));
    }

    /* ------------------------------------------------------------------ tenants */

    @Transactional(readOnly = true)
    public Page<TenantReleaseOperationResponse> tenants(String search, Pageable pageable) {
        return operationsService.tenants(search, pageable);
    }

    @Transactional(readOnly = true)
    public TenantMigrationDetailResponse tenantDetail(Long tenantId) {
        TenantRegistry tenant = findTenant(tenantId);
        TenantReleaseDetailResponse base = operationsService.detail(tenantId);
        TenantHealthResponse health = safeHealth(tenantId);
        TenantScopedFlyway.TenantInfo info = safeInfo(tenant);
        List<ScriptStateResponse> pending = info.pending().stream().map(this::toScriptState).toList();
        return new TenantMigrationDetailResponse(base.tenantId(), base.organizationId(),
                base.organizationName(), base.tenantIdentifier(), base.schemaName(),
                base.applicationVersion(), base.databaseVersion(), base.targetDatabaseVersion(),
                base.catalogVersion(), base.targetCatalogVersion(), base.migrationStatus(),
                base.catalogSyncStatus(), base.maintenanceMode(), base.maintenanceReason(),
                base.healthStatus(), health == null ? null : health.healthScore(),
                health == null ? List.of() : health.issues(),
                info.currentVersion(), pending, info.appliedCount(),
                base.migrationHistory(), base.catalogSyncHistory(),
                base.lastMigrationAt(), base.lastHealthCheckAt(), base.active());
    }

    @Transactional(readOnly = true)
    public MigrationPreviewResponse previewTenant(Long tenantId, String targetVersion) {
        TenantRegistry tenant = findTenant(tenantId);
        String target = StringUtils.hasText(targetVersion) ? targetVersion.trim() : latestAvailableVersion();
        if (!StringUtils.hasText(target)) {
            throw new BadRequestException("No packaged tenant migration version is available");
        }
        TenantScopedFlyway.TenantInfo info = safeInfo(tenant);
        TenantScopedFlyway.ValidationOutcome outcome = safeValidate(tenant);
        String current = info.currentVersion();

        List<ValidationCheckResponse> checks = new ArrayList<>();
        checks.add(new ValidationCheckResponse("flyway_history", "Isolated Flyway history present",
                current == null ? "WARN" : "PASS",
                current == null
                        ? "No tenant history yet; the first migration baselines the schema"
                        : "Current tenant database version is V" + current));

        boolean targetExists = packagedVersions().contains(target);
        checks.add(new ValidationCheckResponse("target_script", "Target script is packaged",
                targetExists ? "PASS" : "FAIL",
                targetExists ? "Flyway script V" + target + " is available" : "No packaged script for V" + target));

        boolean forwardOnly = current == null || compareVersions(target, current) >= 0;
        checks.add(new ValidationCheckResponse("upgrade_path", "Forward-only upgrade",
                forwardOnly ? "PASS" : "FAIL",
                forwardOnly ? "Target is at or above the current version"
                        : "Database downgrades are not supported by this platform"));

        checks.add(new ValidationCheckResponse("flyway_validation", "Flyway validation",
                outcome.valid() ? "PASS" : "FAIL", outcome.message()));

        boolean maintenance = Boolean.TRUE.equals(tenant.getMaintenanceMode());
        checks.add(new ValidationCheckResponse("maintenance_mode", "Maintenance mode",
                maintenance ? "WARN" : "PASS",
                maintenance
                        ? "Tenant is already in maintenance: " + tenant.getMaintenanceReason()
                        : "Tenant is serving traffic; migration will enable maintenance automatically"));

        boolean locked = hasActiveLock(tenantId);
        checks.add(new ValidationCheckResponse("operation_lock", "No concurrent tenant operation",
                locked ? "FAIL" : "PASS",
                locked ? "Another tenant operation holds the database lock" : "No active tenant operation lock"));

        boolean valid = checks.stream().noneMatch(check -> "FAIL".equals(check.status()));
        return new MigrationPreviewResponse(tenant.getId(), tenant.getTenantIdentifier(),
                tenant.getSchemaName(), current, target, info.pendingCount(), info.appliedCount(),
                valid && outcome.valid(), outcome.message(),
                info.pending().stream().map(this::toScriptState).toList(), checks,
                maintenance, LocalDateTime.now());
    }

    public TenantMigrationResponse migrateTenant(Long tenantId, String targetVersion) {
        TenantRegistry tenant = findTenant(tenantId);
        String target = StringUtils.hasText(targetVersion) ? targetVersion.trim() : latestAvailableVersion();
        if (!StringUtils.hasText(target)) {
            throw new BadRequestException("No packaged tenant migration version is available");
        }
        if (compareVersions(target, latestAvailableVersion()) > 0) {
            throw new BadRequestException("Target database version exceeds available tenant migrations");
        }
        log.info("Migration Center requested tenant migration tenant={} target={}",
                tenant.getTenantIdentifier(), target);
        return migrationService.executeTenant(tenantId, target);
    }

    public CatalogSyncExecutionResponse retryCatalogSync(Long tenantId) {
        findTenant(tenantId);
        return catalogSyncService.retryLatestForTenant(tenantId);
    }

    public MigrationExecutionResponse retryTenantMigration(Long tenantId) {
        findTenant(tenantId);
        return migrationService.retryLatestForTenant(tenantId);
    }

    public TenantReleaseOperationResponse maintenance(Long tenantId, MaintenanceModeRequest request) {
        findTenant(tenantId);
        return operationsService.maintenance(tenantId, request);
    }

    /* ------------------------------------------------------------------ scripts */

    @Transactional(readOnly = true)
    public List<MigrationScriptResponse> scripts() {
        List<TenantRegistry> tenants = eligibleTenants();
        Map<Long, Set<String>> applied = appliedVersions(tenants);
        List<MigrationScriptResponse> rows = new ArrayList<>();
        for (TenantScopedFlyway.DiscoveredScript script : tenantFlyway.discover(TENANT_LOCATION)) {
            long appliedCount = applied.values().stream()
                    .filter(versions -> versions.contains(script.version())).count();
            rows.add(new MigrationScriptResponse(script.version(), script.description(), script.script(),
                    script.location(), appliedCount, tenants.size() - appliedCount, tenants.size()));
        }
        return rows;
    }

    /* ------------------------------------------------------------------ catalog */

    @Transactional(readOnly = true)
    public CatalogOverviewResponse catalog() {
        CatalogSyncStatusResponse status = catalogSyncService.status();
        String targetCatalog = releaseRepository.findTopByOrderByCreatedOnDesc()
                .map(PlatformRelease::getTargetCatalogVersion).orElse(null);
        List<CatalogVersionResponse> versions = jdbcTemplate.query("""
                SELECT version_number, change_type, entity_type, entity_key, created_by, created_on
                FROM catalog_versions
                ORDER BY version_number DESC
                LIMIT 100
                """, (rs, rowNum) -> new CatalogVersionResponse(rs.getLong("version_number"),
                rs.getString("change_type"), rs.getString("entity_type"), rs.getString("entity_key"),
                rs.getString("created_by"), toDateTime(rs.getTimestamp("created_on"))));

        Map<Long, String> latestStatus = latestStatus("catalog_sync_executions");
        Map<Long, String> latestError = latestError("catalog_sync_executions");
        Map<Long, LocalDateTime> lastSynced = latestCompletion("catalog_sync_executions");
        List<TenantCatalogRowResponse> tenants = new ArrayList<>();
        for (TenantRegistry tenant : eligibleTenants()) {
            String catalogVersion = tenant.getCatalogVersion() == null
                    ? null : String.valueOf(tenant.getCatalogVersion());
            tenants.add(new TenantCatalogRowResponse(tenant.getId(),
                    tenant.getOrganization() == null ? null : tenant.getOrganization().getOrganizationName(),
                    tenant.getTenantIdentifier(), tenant.getSchemaName(), catalogVersion, targetCatalog,
                    parseStatus(latestStatus.get(tenant.getId())), lastSynced.get(tenant.getId()),
                    latestError.get(tenant.getId())));
        }
        return new CatalogOverviewResponse(status, latestCatalogVersion(), versions, tenants, LocalDateTime.now());
    }

    /* ------------------------------------------------------------------ jobs + history */

    @Transactional(readOnly = true)
    public Page<ExecutionJobResponse> jobs(String jobType, String status, String search, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(jobType)) {
            where.append(" AND job_type = ?");
            args.add(jobType.trim().toUpperCase());
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND status = ?");
            args.add(status.trim().toUpperCase());
        }
        if (StringUtils.hasText(search)) {
            where.append(" AND (LOWER(tenant_identifier) LIKE ? OR LOWER(organization_name) LIKE ?)");
            String needle = "%" + search.trim().toLowerCase() + "%";
            args.add(needle);
            args.add(needle);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + JOB_UNION + ") jobs" + where,
                Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageable.getPageSize());
        pageArgs.add(pageable.getOffset());
        List<ExecutionJobResponse> rows = jdbcTemplate.query(
                "SELECT * FROM (" + JOB_UNION + ") jobs" + where
                        + " ORDER BY activity_at DESC NULLS LAST, job_id DESC LIMIT ? OFFSET ?",
                pageArgs.toArray(), (rs, rowNum) -> new ExecutionJobResponse(
                        rs.getString("job_type") + "-" + rs.getLong("job_id"),
                        rs.getString("job_type"),
                        rs.getObject("tenant_id") == null ? null : rs.getLong("tenant_id"),
                        rs.getString("tenant_identifier"),
                        rs.getString("organization_name"),
                        rs.getString("release_ref"),
                        rs.getString("status"),
                        rs.getInt("attempt"),
                        rs.getString("target_version"),
                        toDateTime(rs.getTimestamp("started_at")),
                        toDateTime(rs.getTimestamp("completed_at")),
                        rs.getString("error_message")));
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> history(String eventType, String search, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(eventType)) {
            where.append(" AND event_type = ?");
            args.add(eventType.trim().toUpperCase());
        }
        if (StringUtils.hasText(search)) {
            where.append(" AND (LOWER(COALESCE(tenant_identifier,'')) LIKE ?"
                    + " OR LOWER(COALESCE(entity_type,'')) LIKE ?"
                    + " OR LOWER(COALESCE(details,'')) LIKE ?)");
            String needle = "%" + search.trim().toLowerCase() + "%";
            args.add(needle);
            args.add(needle);
            args.add(needle);
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operation_audit_events" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageable.getPageSize());
        pageArgs.add(pageable.getOffset());
        List<AuditEventResponse> rows = jdbcTemplate.query("""
                SELECT id, event_type, actor, tenant_identifier, entity_type, entity_id, status,
                       target_version, details, error_message, created_on
                FROM operation_audit_events
                """ + where + " ORDER BY created_on DESC, id DESC LIMIT ? OFFSET ?",
                pageArgs.toArray(), this::mapAuditEvent);
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    /* ------------------------------------------------------------------ helpers */

    private List<TenantRegistry> eligibleTenants() {
        return tenantRepository.findByActiveTrueAndProvisionStatus(ProvisionStatus.COMPLETED);
    }

    private PlatformRelease findRelease(Long id) {
        return releaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found: " + id));
    }

    private TenantRegistry findTenant(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TenantRegistry not found: " + id));
    }

    private ReleaseOverviewResponse toOverview(PlatformRelease release, Map<String, Long> counts) {
        long succeeded = counts.getOrDefault(OperationStatus.SUCCESS.name(), 0L);
        long failed = counts.getOrDefault(OperationStatus.FAILED.name(), 0L);
        long pending = counts.getOrDefault(OperationStatus.PENDING.name(), 0L)
                + counts.getOrDefault(OperationStatus.RUNNING.name(), 0L);
        long total = succeeded + failed + pending;
        int progress = total == 0 ? 0 : (int) Math.round(((succeeded + failed) * 100.0) / total);
        return new ReleaseOverviewResponse(release.getId(), release.getReleaseId(),
                release.getReleaseVersion(), release.getApplicationVersion(),
                release.getTargetDatabaseVersion(), release.getTargetCatalogVersion(), release.getStatus(),
                release.getCreatedOn(), release.getReleasedAt(), release.getCreatedBy(),
                release.getReleaseNotes(), total, succeeded, failed, pending, progress);
    }

    private Map<Long, Map<String, Long>> executionCountsByRelease() {
        Map<Long, Map<String, Long>> counts = new HashMap<>();
        jdbcTemplate.query("""
                SELECT release_id, status, COUNT(*) AS total
                FROM tenant_migration_executions
                WHERE release_id IS NOT NULL
                GROUP BY release_id, status
                """, rs -> {
            counts.computeIfAbsent(rs.getLong("release_id"), key -> new HashMap<>())
                    .put(rs.getString("status"), rs.getLong("total"));
        });
        return counts;
    }

    private Map<String, Long> executionCounts(Long releaseId) {
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query("""
                SELECT status, COUNT(*) AS total
                FROM tenant_migration_executions
                WHERE release_id = ?
                GROUP BY status
                """, rs -> {
            counts.put(rs.getString("status"), rs.getLong("total"));
        }, releaseId);
        return counts;
    }

    private List<ExecutionJobResponse> jobsForRelease(Long releaseId) {
        return jdbcTemplate.query("""
                SELECT e.id, t.id AS tenant_id, t.tenant_identifier, o.organization_name,
                       e.status, e.attempt, e.target_version, e.started_at, e.completed_at, e.error_message
                FROM tenant_migration_executions e
                JOIN tenant_registry t ON t.id = e.tenant_registry_id
                JOIN organizations o ON o.id = t.organization_id
                WHERE e.release_id = ?
                ORDER BY e.id ASC
                """, (rs, rowNum) -> new ExecutionJobResponse("MIGRATION-" + rs.getLong("id"),
                "MIGRATION", rs.getLong("tenant_id"), rs.getString("tenant_identifier"),
                rs.getString("organization_name"), String.valueOf(releaseId), rs.getString("status"),
                rs.getInt("attempt"), rs.getString("target_version"),
                toDateTime(rs.getTimestamp("started_at")), toDateTime(rs.getTimestamp("completed_at")),
                rs.getString("error_message")), releaseId);
    }

    private List<AuditEventResponse> auditForEntity(String entityType, String entityId, int limit) {
        return jdbcTemplate.query("""
                SELECT id, event_type, actor, tenant_identifier, entity_type, entity_id, status,
                       target_version, details, error_message, created_on
                FROM operation_audit_events
                WHERE entity_type = ? AND entity_id = ?
                ORDER BY created_on DESC, id DESC
                LIMIT ?
                """, this::mapAuditEvent, entityType, entityId, limit);
    }

    private AuditEventResponse mapAuditEvent(ResultSet rs, int rowNum) throws SQLException {
        return new AuditEventResponse(rs.getLong("id"), rs.getString("event_type"),
                rs.getString("actor"), rs.getString("tenant_identifier"), rs.getString("entity_type"),
                rs.getString("entity_id"), rs.getString("status"), rs.getString("target_version"),
                rs.getString("details"), rs.getString("error_message"),
                toDateTime(rs.getTimestamp("created_on")));
    }

    private Map<Long, String> latestStatus(String table) {
        requireKnownTable(table);
        Map<Long, String> statuses = new HashMap<>();
        jdbcTemplate.query("""
                SELECT tenant_registry_id, status FROM (
                    SELECT tenant_registry_id, status,
                           ROW_NUMBER() OVER (PARTITION BY tenant_registry_id
                                              ORDER BY created_on DESC, id DESC) AS rn
                    FROM %s) latest
                WHERE rn = 1
                """.formatted(table), rs -> {
            statuses.put(rs.getLong("tenant_registry_id"), rs.getString("status"));
        });
        return statuses;
    }

    private Map<Long, String> latestError(String table) {
        requireKnownTable(table);
        Map<Long, String> errors = new HashMap<>();
        jdbcTemplate.query("""
                SELECT tenant_registry_id, error_message FROM (
                    SELECT tenant_registry_id, error_message,
                           ROW_NUMBER() OVER (PARTITION BY tenant_registry_id
                                              ORDER BY created_on DESC, id DESC) AS rn
                    FROM %s) latest
                WHERE rn = 1
                """.formatted(table), rs -> {
            String message = rs.getString("error_message");
            if (message != null) {
                errors.put(rs.getLong("tenant_registry_id"), message);
            }
        });
        return errors;
    }

    private Map<Long, LocalDateTime> latestCompletion(String table) {
        requireKnownTable(table);
        Map<Long, LocalDateTime> completions = new HashMap<>();
        jdbcTemplate.query("""
                SELECT tenant_registry_id, completed_at FROM (
                    SELECT tenant_registry_id, completed_at,
                           ROW_NUMBER() OVER (PARTITION BY tenant_registry_id
                                              ORDER BY created_on DESC, id DESC) AS rn
                    FROM %s) latest
                WHERE rn = 1
                """.formatted(table), rs -> {
            Timestamp completedAt = rs.getTimestamp("completed_at");
            if (completedAt != null) {
                completions.put(rs.getLong("tenant_registry_id"), completedAt.toLocalDateTime());
            }
        });
        return completions;
    }

    private void requireKnownTable(String table) {        if (!"tenant_migration_executions".equals(table) && !"catalog_sync_executions".equals(table)) {
            throw new IllegalArgumentException("Unsupported execution table: " + table);
        }
    }

    private long countExecutions(String table, String predicate) {
        requireKnownTable(table);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + predicate, Long.class);
        return count == null ? 0 : count;
    }

    private String latestAvailableVersion() {
        return packagedVersions().stream().reduce((left, right) ->
                compareVersions(left, right) >= 0 ? left : right).orElse(null);
    }

    private Set<String> packagedVersions() {
        Set<String> versions = new HashSet<>();
        for (TenantScopedFlyway.DiscoveredScript script : tenantFlyway.discover(TENANT_LOCATION)) {
            versions.add(script.version());
        }
        return versions;
    }

    private String latestCatalogVersion() {
        Long version = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version_number), 0) FROM catalog_versions", Long.class);
        return String.valueOf(version == null ? 0 : version);
    }

    private Map<Long, Set<String>> appliedVersions(List<TenantRegistry> tenants) {
        Map<Long, Set<String>> applied = new HashMap<>();
        for (TenantRegistry tenant : tenants) {
            String schema = tenant.getSchemaName();
            if (schema == null || !TENANT_SCHEMA.matcher(schema).matches()) {
                applied.put(tenant.getId(), Set.of());
                continue;
            }
            try {
                List<String> versions = jdbcTemplate.queryForList(
                        "SELECT version FROM " + schema
                                + ".flyway_tenant_schema_history WHERE success = true AND version IS NOT NULL",
                        String.class);
                applied.put(tenant.getId(), new HashSet<>(versions));
            } catch (Exception ex) {
                log.debug("Unable to read Flyway history for schema {}", schema, ex);
                applied.put(tenant.getId(), Set.of());
            }
        }
        return applied;
    }

    private boolean hasActiveLock(Long tenantId) {
        Long locks = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM tenant_operation_locks
                WHERE tenant_registry_id = ? AND expires_at > now()
                """, Long.class, tenantId);
        return locks != null && locks > 0;
    }

    private String safeCurrentVersion(TenantRegistry tenant) {
        try {
            return tenantFlyway.currentVersion(tenant);
        } catch (Exception ex) {
            log.debug("Unable to read Flyway version for {}", tenant.getTenantIdentifier(), ex);
            return null;
        }
    }

    private TenantScopedFlyway.TenantInfo safeInfo(TenantRegistry tenant) {
        try {
            return tenantFlyway.info(tenant);
        } catch (Exception ex) {
            log.warn("Unable to read Flyway inventory for {}", tenant.getTenantIdentifier(), ex);
            return new TenantScopedFlyway.TenantInfo(tenant.getSchemaName(), null,
                    List.of(), List.of(), 0, 0);
        }
    }

    private TenantHealthResponse safeHealth(Long tenantId) {
        try {
            return healthService.check(tenantId);
        } catch (Exception ex) {
            log.warn("Unable to compute tenant health for tenant {}", tenantId, ex);
            return null;
        }
    }

    private TenantScopedFlyway.ValidationOutcome safeValidate(TenantRegistry tenant) {        try {
            return tenantFlyway.validate(tenant);
        } catch (Exception ex) {
            return new TenantScopedFlyway.ValidationOutcome(false,
                    "Flyway validation could not run: " + ex.getMessage());
        }
    }

    private ScriptStateResponse toScriptState(TenantScopedFlyway.ScriptState state) {
        return new ScriptStateResponse(state.version(), state.description(), state.script(),
                state.state(), state.checksum(), state.installedOn(), state.executionTimeMs());
    }

    private OperationStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return OperationStatus.PENDING;
        }
        try {
            return OperationStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return OperationStatus.PENDING;
        }
    }

    private static LocalDateTime toDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static int compareVersions(String left, String right) {
        if (left == null) {
            return right == null ? 0 : -1;
        }
        if (right == null) {
            return 1;
        }
        String[] leftParts = left.split("[._]");
        String[] rightParts = right.split("[._]");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            String leftPart = index < leftParts.length ? leftParts[index] : "0";
            String rightPart = index < rightParts.length ? rightParts[index] : "0";
            int comparison;
            try {
                comparison = Long.compare(Long.parseLong(leftPart), Long.parseLong(rightPart));
            } catch (NumberFormatException ex) {
                comparison = leftPart.compareTo(rightPart);
            }
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }
}
