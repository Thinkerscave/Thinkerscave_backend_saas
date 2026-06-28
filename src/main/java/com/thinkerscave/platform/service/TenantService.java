package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.response.TenantRegistryResponse;
import com.thinkerscave.platform.enums.ProvisionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantService {

    Page<TenantRegistryResponse> getTenants(ProvisionStatus status, String search, Pageable pageable);

    TenantRegistryResponse getTenantById(Long id);

    TenantRegistryResponse setMaintenanceMode(Long id);

    TenantRegistryResponse resumeTenant(Long id);

    void triggerBackup(Long id);

    void triggerMigration(Long id);
}
