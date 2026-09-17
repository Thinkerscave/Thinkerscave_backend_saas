package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.CreateReleaseRequest;
import com.thinkerscave.platform.dto.response.ReleaseResponse;
import com.thinkerscave.platform.dto.response.ReleaseSummaryResponse;
import com.thinkerscave.platform.entity.PlatformRelease;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.OperationStatus;
import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.platform.enums.ReleaseStatus;
import com.thinkerscave.platform.repository.PlatformReleaseRepository;
import com.thinkerscave.platform.repository.TenantMigrationExecutionRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseService {
    private final PlatformReleaseRepository releaseRepository;
    private final TenantMigrationExecutionRepository executionRepository;
    private final TenantMigrationService migrationService;
    private final TenantRegistryRepository tenantRepository;
    private final TenantCatalogSyncService catalogSyncService;
    private final JdbcTemplate jdbcTemplate;

    public ReleaseResponse create(CreateReleaseRequest request) {
        PlatformRelease release = PlatformRelease.builder()
                .releaseId("REL-" + request.releaseVersion() + "-" + UUID.randomUUID().toString().substring(0, 8))
                .releaseVersion(request.releaseVersion())
                .applicationVersion(request.applicationVersion())
                .targetDatabaseVersion(request.targetDatabaseVersion())
                .targetCatalogVersion(request.targetCatalogVersion())
                .releaseNotes(request.releaseNotes())
                .status(ReleaseStatus.DRAFT)
                .build();
        PlatformRelease saved = releaseRepository.save(release);
        audit("RELEASE_CREATED", saved, "SUCCESS", null);
        return response(saved);
    }

    public ReleaseResponse execute(Long id) {
        PlatformRelease release = find(id);
        if (release.getStatus() == ReleaseStatus.RELEASED || release.getStatus() == ReleaseStatus.READY) {
            throw new BadRequestException("Release is already released or currently executing");
        }
        if (tenantRepository.findByActiveTrueAndProvisionStatus(ProvisionStatus.COMPLETED).isEmpty()) {
            throw new BadRequestException("No eligible provisioned tenants exist for this release");
        }
        validateCatalogTarget(release.getTargetCatalogVersion());
        if (releaseRepository.claimExecution(id) != 1) {
            throw new BadRequestException("Release execution could not be claimed");
        }
        PlatformRelease executingRelease = find(id);
        audit("RELEASE_EXECUTION_STARTED", executingRelease, "RUNNING", null);
        var results = migrationService.executeRelease(executingRelease);
        boolean catalogFailed = false;
        for (var result : results) {
            if (result.status() != OperationStatus.SUCCESS) continue;
            try {
                tenantRepository.findById(result.tenantId()).ifPresent(tenant -> {
                    catalogSyncService.reconcileReleaseTarget(tenant, executingRelease.getTargetCatalogVersion());
                    TenantRegistry refreshed = tenantRepository.findById(tenant.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found after catalog sync"));
                    refreshed.setApplicationVersion(executingRelease.getApplicationVersion());
                    tenantRepository.saveAndFlush(refreshed);
                });
            } catch (Exception ex) {
                catalogFailed = true;
            }
        }
        boolean failed = results.stream().anyMatch(result -> result.status() == OperationStatus.FAILED);
        executingRelease.setStatus(failed || catalogFailed ? ReleaseStatus.FAILED : ReleaseStatus.RELEASED);
        executingRelease.setReleasedAt(failed || catalogFailed ? null : LocalDateTime.now());
        PlatformRelease saved = releaseRepository.save(executingRelease);
        audit(saved.getStatus() == ReleaseStatus.RELEASED
                ? "RELEASE_EXECUTION_COMPLETED" : "RELEASE_EXECUTION_FAILED",
                saved, saved.getStatus() == ReleaseStatus.RELEASED ? "SUCCESS" : "FAILED",
                saved.getStatus() == ReleaseStatus.RELEASED ? null : "One or more tenant operations failed");
        return response(saved);
    }

    public Page<ReleaseResponse> history(Pageable pageable) {
        return releaseRepository.findAll(pageable).map(this::response);
    }

    public ReleaseResponse current() {
        return releaseRepository.findTopByOrderByCreatedOnDesc().map(this::response).orElse(null);
    }

    public ReleaseSummaryResponse summary() {
        var tenants = tenantRepository.findByActiveTrueAndProvisionStatus(ProvisionStatus.COMPLETED);
        PlatformRelease release = releaseRepository.findTopByOrderByCreatedOnDesc().orElse(null);
        long upToDate = release == null ? 0 : tenants.stream().filter(t ->
                release.getApplicationVersion().equals(t.getApplicationVersion())
                        && release.getTargetDatabaseVersion().equals(t.getObservedDatabaseVersion())
                        && release.getTargetCatalogVersion().equals(String.valueOf(t.getCatalogVersion()))).count();
        long failed = tenants.stream().filter(t -> Boolean.TRUE.equals(t.getMaintenanceMode())
                || migrationRepositoryFailed(t.getId())).count();
        long pending = tenants.stream().filter(t -> {
            boolean current = release != null
                    && release.getApplicationVersion().equals(t.getApplicationVersion())
                    && release.getTargetDatabaseVersion().equals(t.getObservedDatabaseVersion())
                    && release.getTargetCatalogVersion().equals(String.valueOf(t.getCatalogVersion()));
            return !current && !Boolean.TRUE.equals(t.getMaintenanceMode()) && !migrationRepositoryFailed(t.getId());
        }).count();
        return new ReleaseSummaryResponse(release == null ? null : response(release), tenants.size(),
                upToDate, pending, failed);
    }

    private PlatformRelease find(Long id) {
        return releaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found: " + id));
    }

    private ReleaseResponse response(PlatformRelease release) {
        return new ReleaseResponse(release.getId(), release.getReleaseId(), release.getReleaseVersion(),
                release.getApplicationVersion(), release.getTargetDatabaseVersion(),
                release.getTargetCatalogVersion(), release.getStatus(), release.getCreatedOn(),
                release.getReleasedAt(), release.getCreatedBy(), release.getReleaseNotes());
    }

    private boolean migrationRepositoryFailed(Long tenantId) {
        return executionRepository.findTopByTenant_IdOrderByCreatedOnDesc(tenantId)
                .map(execution -> execution.getStatus() == OperationStatus.FAILED)
                .orElse(false);
    }

    private void validateCatalogTarget(String target) {
        Long current = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version_number),0) FROM catalog_versions", Long.class);
        try {
            if (Long.parseLong(target) != (current == null ? 0 : current)) {
                throw new BadRequestException(
                        "Target catalog version must equal the current published catalog");
            }
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Target catalog version must be numeric");
        }
    }

    private void audit(String eventType, PlatformRelease release, String status, String error) {
        jdbcTemplate.update("""
                INSERT INTO operation_audit_events
                    (event_type, actor, entity_type, entity_id, execution_id,
                     status, target_version, error_message, details)
                VALUES (?, 'system', 'RELEASE', ?, ?, ?, ?, ?, ?)
                """, eventType, String.valueOf(release.getId()), release.getId(), status,
                release.getTargetDatabaseVersion(), error, release.getReleaseVersion());
    }
}
