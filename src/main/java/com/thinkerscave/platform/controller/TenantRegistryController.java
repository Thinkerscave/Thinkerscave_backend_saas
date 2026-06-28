package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.TenantRegistryResponse;
import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.platform.service.TenantService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/tenant-registry")
@RequiredArgsConstructor
@Tag(name = "Tenant Registry", description = "Manage tenant health, migration and backup")
public class TenantRegistryController {

    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List tenant registries")
    public ResponseEntity<ApiResponse<Page<TenantRegistryResponse>>> getTenants(
            @RequestParam(required = false) ProvisionStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Tenant registry retrieved",
                tenantService.getTenants(status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get tenant detail")
    public ResponseEntity<ApiResponse<TenantRegistryResponse>> getTenantById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tenant retrieved", tenantService.getTenantById(id)));
    }

    @PostMapping("/{id}/maintenance")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Put tenant in maintenance mode")
    public ResponseEntity<ApiResponse<TenantRegistryResponse>> setMaintenanceMode(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tenant in maintenance mode", tenantService.setMaintenanceMode(id)));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Resume tenant from maintenance mode")
    public ResponseEntity<ApiResponse<TenantRegistryResponse>> resumeTenant(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tenant resumed", tenantService.resumeTenant(id)));
    }

    @PostMapping("/{id}/backup")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Trigger tenant backup")
    public ResponseEntity<ApiResponse<Void>> triggerBackup(@PathVariable Long id) {
        tenantService.triggerBackup(id);
        return ResponseEntity.ok(ApiResponse.noContent("Backup triggered"));
    }

    @PostMapping("/{id}/migrate")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Trigger tenant migration")
    public ResponseEntity<ApiResponse<Void>> triggerMigration(@PathVariable Long id) {
        tenantService.triggerMigration(id);
        return ResponseEntity.ok(ApiResponse.noContent("Migration triggered"));
    }
}
