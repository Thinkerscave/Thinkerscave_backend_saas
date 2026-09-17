package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.MaintenanceModeRequest;
import com.thinkerscave.platform.dto.response.*;
import com.thinkerscave.platform.entity.PlatformRelease;
import com.thinkerscave.platform.entity.TenantMigrationExecution;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.OperationStatus;
import com.thinkerscave.platform.repository.PlatformReleaseRepository;
import com.thinkerscave.platform.repository.TenantMigrationExecutionRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantReleaseOperationsService {
    private final TenantRegistryRepository tenantRepository;
    private final PlatformReleaseRepository releaseRepository;
    private final TenantMigrationExecutionRepository migrationRepository;
    private final TenantMigrationService migrationService;
    private final TenantCatalogSyncService catalogSyncService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public Page<TenantReleaseOperationResponse> tenants(String search, Pageable pageable) {
        return tenantRepository.searchTenants(null, search, pageable).map(this::operation);
    }

    @Transactional(readOnly = true)
    public TenantReleaseDetailResponse detail(Long tenantId) {
        TenantRegistry tenant = find(tenantId);
        TenantReleaseOperationResponse row = operation(tenant);
        return new TenantReleaseDetailResponse(row.tenantId(), row.organizationId(), row.organizationName(),
                row.tenantIdentifier(), row.schemaName(), row.applicationVersion(), row.databaseVersion(),
                row.targetDatabaseVersion(), row.catalogVersion(), row.targetCatalogVersion(),
                row.migrationStatus(), row.catalogSyncStatus(), row.maintenanceMode(),
                row.maintenanceReason(), row.maintenanceStartedAt(), row.healthStatus(),
                row.lastMigrationAt(), row.lastHealthCheckAt(), null, Boolean.TRUE.equals(tenant.getActive()),
                migrationService.historyForTenant(tenantId), catalogSyncService.historyForTenant(tenantId));
    }

    @Transactional
    public TenantReleaseOperationResponse maintenance(Long tenantId, MaintenanceModeRequest request) {
        TenantRegistry tenant = find(tenantId);
        tenant.setMaintenanceMode(request.enabled());
        tenant.setMaintenanceReason(Boolean.TRUE.equals(request.enabled()) ? request.reason() : null);
        tenant.setMaintenanceOperation(Boolean.TRUE.equals(request.enabled()) ? "MANUAL" : null);
        tenant.setMaintenanceStartedAt(Boolean.TRUE.equals(request.enabled()) ? LocalDateTime.now() : null);
        TenantRegistry saved = tenantRepository.save(tenant);
        jdbcTemplate.update("""
                INSERT INTO operation_audit_events
                    (event_type, actor, tenant_identifier, entity_type, entity_id, status, details)
                VALUES (?, 'system', ?, 'TENANT', ?, 'SUCCESS', ?)
                """, Boolean.TRUE.equals(request.enabled())
                        ? "TENANT_MAINTENANCE_ENABLED" : "TENANT_MAINTENANCE_DISABLED",
                saved.getTenantIdentifier(), String.valueOf(saved.getId()), request.reason());
        return operation(saved);
    }

    private TenantReleaseOperationResponse operation(TenantRegistry tenant) {
        PlatformRelease release = releaseRepository.findTopByOrderByCreatedOnDesc().orElse(null);
        TenantMigrationExecution migration = migrationRepository
                .findTopByTenant_IdOrderByCreatedOnDesc(tenant.getId()).orElse(null);
        List<CatalogSyncExecutionResponse> catalogHistory = catalogSyncService.historyForTenant(tenant.getId());
        OperationStatus catalogStatus = catalogHistory.isEmpty()
                ? OperationStatus.PENDING : catalogHistory.get(0).status();
        return new TenantReleaseOperationResponse(tenant.getId(), tenant.getOrganization().getId(),
                tenant.getOrganization().getOrganizationName(), tenant.getTenantIdentifier(),
                tenant.getSchemaName(), tenant.getApplicationVersion(), tenant.getObservedDatabaseVersion(),
                release == null ? null : release.getTargetDatabaseVersion(),
                tenant.getCatalogVersion() == null ? null : String.valueOf(tenant.getCatalogVersion()),
                release == null ? null : release.getTargetCatalogVersion(),
                migration == null ? OperationStatus.PENDING : migration.getStatus(), catalogStatus,
                Boolean.TRUE.equals(tenant.getMaintenanceMode()), tenant.getMaintenanceReason(),
                tenant.getMaintenanceStartedAt(), normalizeHealth(tenant),
                tenant.getLastMigrationAt(), tenant.getLastHealthCheckAt());
    }

    private String normalizeHealth(TenantRegistry tenant) {
        if (Boolean.TRUE.equals(tenant.getMaintenanceMode())) return "MAINTENANCE";
        return switch (String.valueOf(tenant.getHealthStatus())) {
            case "HEALTHY", "WARNING", "CRITICAL" -> tenant.getHealthStatus();
            case "DEGRADED" -> "WARNING";
            case "UNHEALTHY" -> "CRITICAL";
            default -> "WARNING";
        };
    }

    private TenantRegistry find(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TenantRegistry not found: " + id));
    }
}
