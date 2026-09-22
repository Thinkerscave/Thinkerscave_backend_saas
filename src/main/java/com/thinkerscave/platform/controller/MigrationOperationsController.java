package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.MaintenanceModeRequest;
import com.thinkerscave.platform.dto.response.*;
import com.thinkerscave.platform.service.TenantMigrationService;
import com.thinkerscave.platform.service.TenantReleaseOperationsService;
import com.thinkerscave.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/migrations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class MigrationOperationsController {
    private final TenantReleaseOperationsService operationsService;
    private final TenantMigrationService migrationService;

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<Page<TenantReleaseOperationResponse>>> tenants(
            @RequestParam(required = false) String search, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Tenant migrations retrieved",
                operationsService.tenants(search, pageable)));
    }

    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<ApiResponse<TenantReleaseDetailResponse>> detail(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Tenant migration detail retrieved",
                operationsService.detail(tenantId)));
    }

    @GetMapping("/tenants/{tenantId}/history")
    public ResponseEntity<ApiResponse<List<MigrationExecutionResponse>>> history(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Migration history retrieved",
                migrationService.historyForTenant(tenantId)));
    }

    @PostMapping({"/tenants/{tenantId}/retry", "/tenants/{tenantId}/retry-by-tenant"})
    public ResponseEntity<ApiResponse<MigrationExecutionResponse>> retry(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Migration retry completed",
                migrationService.retryLatestForTenant(tenantId)));
    }

    @PutMapping("/tenants/{tenantId}/maintenance")
    public ResponseEntity<ApiResponse<TenantReleaseOperationResponse>> maintenance(
            @PathVariable Long tenantId, @Valid @RequestBody MaintenanceModeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Maintenance state updated",
                operationsService.maintenance(tenantId, request)));
    }
}
