package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.response.TenantRegistryResponse;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.platform.service.TenantService;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService {

    private final TenantRegistryRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TenantRegistryResponse> getTenants(ProvisionStatus status, String search, Pageable pageable) {
        return tenantRepository.searchTenants(status, search, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantRegistryResponse getTenantById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public TenantRegistryResponse setMaintenanceMode(Long id) {
        TenantRegistry tenant = findById(id);
        tenant.setMaintenanceMode(true);
        log.info("Tenant set to maintenance mode: {}", tenant.getTenantIdentifier());
        return toResponse(tenantRepository.save(tenant));
    }

    @Override
    @Transactional
    public TenantRegistryResponse resumeTenant(Long id) {
        TenantRegistry tenant = findById(id);
        tenant.setMaintenanceMode(false);
        log.info("Tenant resumed from maintenance mode: {}", tenant.getTenantIdentifier());
        return toResponse(tenantRepository.save(tenant));
    }

    @Override
    @Transactional
    public void triggerBackup(Long id) {
        TenantRegistry tenant = findById(id);
        // In dev: log the operation — in production this triggers actual backup
        log.info("Backup triggered for tenant: {}", tenant.getTenantIdentifier());
        tenant.setLastBackupAt(java.time.LocalDateTime.now());
        tenantRepository.save(tenant);
    }

    @Override
    @Transactional
    public void triggerMigration(Long id) {
        TenantRegistry tenant = findById(id);
        // In dev: log the operation — in production this triggers Flyway migration
        log.info("Migration triggered for tenant: {}", tenant.getTenantIdentifier());
        tenant.setLastMigrationAt(java.time.LocalDateTime.now());
        tenantRepository.save(tenant);
    }

    private TenantRegistry findById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TenantRegistry not found: " + id));
    }

    private TenantRegistryResponse toResponse(TenantRegistry t) {
        return TenantRegistryResponse.builder()
                .id(t.getId())
                .tenantIdentifier(t.getTenantIdentifier())
                .organizationId(t.getOrganization().getId())
                .organizationName(t.getOrganization().getOrganizationName())
                .schemaName(t.getSchemaName())
                .databaseVersion(t.getDatabaseVersion())
                .migrationVersion(t.getMigrationVersion())
                .templateVersion(t.getTemplateVersion())
                .provisionStatus(t.getProvisionStatus())
                .databaseSizeMb(t.getDatabaseSizeMb())
                .storageUsedMb(t.getStorageUsedMb())
                .lastMigrationAt(t.getLastMigrationAt())
                .lastBackupAt(t.getLastBackupAt())
                .lastHealthCheckAt(t.getLastHealthCheckAt())
                .tenantDomain(t.getTenantDomain())
                .customDomain(t.getCustomDomain())
                .maintenanceMode(t.getMaintenanceMode())
                .active(t.getActive())
                .remarks(t.getRemarks())
                .createdOn(t.getCreatedOn())
                .createdBy(t.getCreatedBy())
                .updatedOn(t.getUpdatedOn())
                .updatedBy(t.getUpdatedBy())
                .build();
    }
}
