package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.response.TenantMigrationResponse;
import com.thinkerscave.platform.dto.response.MigrationExecutionResponse;
import com.thinkerscave.platform.entity.PlatformRelease;
import com.thinkerscave.platform.entity.TenantMigrationExecution;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.OperationStatus;
import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.platform.repository.PlatformReleaseRepository;
import com.thinkerscave.platform.repository.TenantMigrationExecutionRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationService {
    private final TenantRegistryRepository tenantRepository;
    private final TenantMigrationExecutionRepository executionRepository;
    private final PlatformReleaseRepository releaseRepository;
    private final TenantScopedFlyway tenantFlyway;
    private final JdbcTemplate jdbcTemplate;

    public List<TenantMigrationResponse> executeRelease(PlatformRelease release) {
        return tenantRepository.findByActiveTrueAndProvisionStatus(ProvisionStatus.COMPLETED).stream()
                .map(tenant -> execute(tenant, release, release.getTargetDatabaseVersion(), 1))
                .map(this::toResponse)
                .toList();
    }

    public TenantMigrationResponse executeTenant(Long tenantId, String targetVersion) {
        TenantRegistry tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TenantRegistry not found: " + tenantId));
        return toResponse(execute(tenant, null, targetVersion, 1));
    }

    public TenantMigrationResponse retry(Long executionId) {
        TenantMigrationExecution previous = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Migration execution not found: " + executionId));
        if (previous.getStatus() != OperationStatus.FAILED) {
            throw new BadRequestException("Only failed migration executions may be retried");
        }
        return toResponse(execute(previous.getTenant(), previous.getRelease(),
                previous.getTargetVersion(), previous.getAttempt() + 1));
    }

    public MigrationExecutionResponse retryLatestForTenant(Long tenantId) {
        TenantMigrationExecution previous = executionRepository
                .findTopByTenant_IdAndStatusOrderByCreatedOnDesc(tenantId, OperationStatus.FAILED)
                .orElseThrow(() -> new BadRequestException("Tenant has no failed migration execution"));
        return toContract(execute(previous.getTenant(), previous.getRelease(),
                previous.getTargetVersion(), previous.getAttempt() + 1));
    }

    public List<MigrationExecutionResponse> historyForTenant(Long tenantId) {
        return executionRepository.findByTenant_IdOrderByCreatedOnDesc(
                        tenantId, Pageable.unpaged()).stream().map(this::toContract).toList();
    }

    public Page<TenantMigrationResponse> history(Long tenantId, Pageable pageable) {
        return executionRepository.findByTenant_IdOrderByCreatedOnDesc(tenantId, pageable).map(this::toResponse);
    }

    private TenantMigrationExecution execute(
            TenantRegistry tenant, PlatformRelease release, String targetVersion, int attempt) {
        TenantMigrationExecution execution = executionRepository.save(TenantMigrationExecution.builder()
                .release(release)
                .tenant(tenant)
                .targetVersion(targetVersion)
                .attempt(attempt)
                .status(OperationStatus.PENDING)
                .build());
        String owner = UUID.randomUUID().toString();
        if (!acquireLock(tenant.getId(), execution.getId(), owner)) {
            return fail(execution, "Another tenant operation holds the database lock");
        }

        try {
            execution.setStatus(OperationStatus.RUNNING);
            execution.setStartedAt(LocalDateTime.now());
            execution.setFromVersion(tenantFlyway.currentVersion(tenant));
            execution = executionRepository.saveAndFlush(execution);
            tenant = setMaintenance(tenant, true, "RELEASE_MIGRATION", "Migration to " + targetVersion);
            audit("TENANT_MIGRATION_STARTED", tenant, execution, OperationStatus.RUNNING, null);

            TenantScopedFlyway.MigrationRun run = tenantFlyway.migrate(tenant, targetVersion);
            persistMigrationResults(execution.getId(), run.migrationInfo());
            execution.setFromVersion(run.beforeVersion());
            execution.setObservedVersion(run.afterVersion());
            execution.setStatus(OperationStatus.SUCCESS);
            execution.setCompletedAt(LocalDateTime.now());
            tenant.setObservedDatabaseVersion(run.afterVersion());
            tenant.setMigrationVersion(run.afterVersion());
            tenant.setLastMigrationAt(LocalDateTime.now());
            tenant = setMaintenance(tenant, false, null, null);
            audit("TENANT_MIGRATION_COMPLETED", tenant, execution, OperationStatus.SUCCESS, null);
            return executionRepository.save(execution);
        } catch (Exception ex) {
            log.error("Tenant migration failed tenant={} target={}", tenant.getTenantIdentifier(), targetVersion, ex);
            try {
                execution.setObservedVersion(tenantFlyway.currentVersion(tenant));
            } catch (Exception ignored) {
                // Preserve the primary migration error.
            }
            jdbcTemplate.update("""
                    INSERT INTO tenant_migration_results
                        (execution_id, description, status, error_message, installed_on)
                    VALUES (?, 'Flyway migration failure', 'FAILED', ?, now())
                    """, execution.getId(), sanitize(ex));
            audit("TENANT_MIGRATION_FAILED", tenant, execution, OperationStatus.FAILED, sanitize(ex));
            return fail(execution, sanitize(ex)); // maintenance deliberately remains enabled
        } finally {
            releaseLock(tenant.getId(), owner);
        }
    }

    private boolean acquireLock(Long tenantId, Long executionId, String owner) {
        try {
            String token = jdbcTemplate.queryForObject("""
                    INSERT INTO tenant_operation_locks
                        (tenant_registry_id, operation_type, execution_id, owner_token, acquired_at, expires_at)
                    VALUES (?, 'MIGRATION', ?, ?, now(), now() + interval '30 minutes')
                    ON CONFLICT (tenant_registry_id) DO UPDATE SET
                        operation_type=EXCLUDED.operation_type, execution_id=EXCLUDED.execution_id,
                        owner_token=EXCLUDED.owner_token, acquired_at=EXCLUDED.acquired_at,
                        expires_at=EXCLUDED.expires_at
                    WHERE tenant_operation_locks.expires_at < now()
                    RETURNING owner_token
                    """, String.class, tenantId, executionId, owner);
            return owner.equals(token);
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    private void releaseLock(Long tenantId, String owner) {
        jdbcTemplate.update("DELETE FROM tenant_operation_locks WHERE tenant_registry_id=? AND owner_token=?",
                tenantId, owner);
    }

    private TenantRegistry setMaintenance(TenantRegistry tenant, boolean enabled, String operation, String reason) {
        tenant.setMaintenanceMode(enabled);
        tenant.setMaintenanceOperation(operation);
        tenant.setMaintenanceReason(reason);
        tenant.setMaintenanceStartedAt(enabled ? LocalDateTime.now() : null);
        return tenantRepository.saveAndFlush(tenant);
    }

    private TenantMigrationExecution fail(TenantMigrationExecution execution, String message) {
        execution.setStatus(OperationStatus.FAILED);
        execution.setErrorMessage(message);
        execution.setCompletedAt(LocalDateTime.now());
        return executionRepository.save(execution);
    }

    private void persistMigrationResults(Long executionId, List<MigrationInfo> migrations) {
        for (MigrationInfo migration : migrations) {
            if (migration.getState().isApplied()) {
                jdbcTemplate.update("""
                        INSERT INTO tenant_migration_results
                            (execution_id, migration_version, description, script, execution_time_ms,
                             status, installed_on)
                        VALUES (?, ?, ?, ?, ?, 'SUCCESS', ?)
                        """, executionId,
                        migration.getVersion() == null ? null : migration.getVersion().getVersion(),
                        migration.getDescription(), migration.getScript(), migration.getExecutionTime(),
                        migration.getInstalledOn());
            }
        }
    }

    private void audit(String type, TenantRegistry tenant, TenantMigrationExecution execution,
                       OperationStatus status, String error) {
        jdbcTemplate.update("""
                INSERT INTO operation_audit_events
                    (event_type, actor, tenant_identifier, entity_type, entity_id,
                     execution_id, status, target_version, error_message)
                VALUES (?, 'system', ?, 'TENANT', ?, ?, ?, ?, ?)
                """, type, tenant.getTenantIdentifier(), tenant.getId().toString(), execution.getId(),
                status.name(), execution.getTargetVersion(), error);
    }

    private String sanitize(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }

    public TenantMigrationResponse toResponse(TenantMigrationExecution execution) {
        TenantRegistry tenant = tenantRepository.findById(execution.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("TenantRegistry not found"));
        return new TenantMigrationResponse(execution.getId(),
                execution.getRelease() == null ? null : execution.getRelease().getId(),
                tenant.getId(), tenant.getTenantIdentifier(),
                execution.getFromVersion(), execution.getTargetVersion(), execution.getObservedVersion(),
                execution.getStatus(), execution.getAttempt(), execution.getStartedAt(),
                execution.getCompletedAt(), execution.getErrorMessage());
    }

    public MigrationExecutionResponse toContract(TenantMigrationExecution execution) {
        TenantRegistry tenant = tenantRepository.findById(execution.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("TenantRegistry not found"));
        boolean failed = execution.getStatus() == OperationStatus.FAILED;
        String releaseId = execution.getRelease() == null ? null
                : releaseRepository.findById(execution.getRelease().getId())
                        .map(PlatformRelease::getReleaseId)
                        .orElse(null);
        return new MigrationExecutionResponse(String.valueOf(execution.getId()), tenant.getId(),
                releaseId,
                execution.getFromVersion(), execution.getTargetVersion(), execution.getObservedVersion(),
                null, execution.getStatus(), execution.getStartedAt(), execution.getCompletedAt(),
                failed ? execution.getCompletedAt() : null, failed ? execution.getObservedVersion() : null,
                execution.getErrorMessage(), Math.max(0, execution.getAttempt() - 1));
    }
}
