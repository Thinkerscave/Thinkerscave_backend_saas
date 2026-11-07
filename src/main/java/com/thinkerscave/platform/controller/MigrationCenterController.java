package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.CreateReleaseRequest;
import com.thinkerscave.platform.dto.request.MaintenanceModeRequest;
import com.thinkerscave.platform.dto.response.AuditEventResponse;
import com.thinkerscave.platform.dto.response.CatalogOverviewResponse;
import com.thinkerscave.platform.dto.response.CatalogSyncExecutionResponse;
import com.thinkerscave.platform.dto.response.ExecutionJobResponse;
import com.thinkerscave.platform.dto.response.MigrationCenterOverviewResponse;
import com.thinkerscave.platform.dto.response.MigrationExecutionResponse;
import com.thinkerscave.platform.dto.response.MigrationPreviewResponse;
import com.thinkerscave.platform.dto.response.MigrationScriptResponse;
import com.thinkerscave.platform.dto.response.ReleaseDetailResponse;
import com.thinkerscave.platform.dto.response.ReleaseOverviewResponse;
import com.thinkerscave.platform.dto.response.ReleaseValidationResponse;
import com.thinkerscave.platform.dto.response.TenantMigrationDetailResponse;
import com.thinkerscave.platform.dto.response.TenantMigrationResponse;
import com.thinkerscave.platform.dto.response.TenantReleaseOperationResponse;
import com.thinkerscave.platform.service.MigrationCenterService;
import com.thinkerscave.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Migration Center API. The UI reads the three lifecycles from here; write operations delegate to
 * the existing, audited migration and catalog synchronization services.
 */
@RestController
@RequestMapping("/api/platform/migration-center")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class MigrationCenterController {
    private final MigrationCenterService service;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<MigrationCenterOverviewResponse>> overview() {
        return ResponseEntity.ok(ApiResponse.success("Migration Center overview retrieved", service.overview()));
    }

    /* ------------------------------------------------------------------ releases */

    @GetMapping("/releases")
    public ResponseEntity<ApiResponse<Page<ReleaseOverviewResponse>>> releases(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Releases retrieved", service.releases(pageable)));
    }

    @PostMapping("/releases")
    public ResponseEntity<ApiResponse<ReleaseOverviewResponse>> createRelease(
            @Valid @RequestBody CreateReleaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Release created", service.createRelease(request)));
    }

    @GetMapping("/releases/{id}")
    public ResponseEntity<ApiResponse<ReleaseDetailResponse>> releaseDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Release detail retrieved", service.releaseDetail(id)));
    }

    @PostMapping("/releases/{id}/validate")
    public ResponseEntity<ApiResponse<ReleaseValidationResponse>> validateRelease(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Release validation completed", service.validateRelease(id)));
    }

    @PostMapping("/releases/{id}/execute")
    public ResponseEntity<ApiResponse<ReleaseOverviewResponse>> executeRelease(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Release execution finished", service.executeRelease(id)));
    }

    /* ------------------------------------------------------------------ tenants */

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<Page<TenantReleaseOperationResponse>>> tenants(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Tenant migrations retrieved",
                service.tenants(search, pageable)));
    }

    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<ApiResponse<TenantMigrationDetailResponse>> tenantDetail(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Tenant migration detail retrieved",
                service.tenantDetail(tenantId)));
    }

    @GetMapping("/tenants/{tenantId}/preview")
    public ResponseEntity<ApiResponse<MigrationPreviewResponse>> preview(
            @PathVariable Long tenantId, @RequestParam(required = false) String targetVersion) {
        return ResponseEntity.ok(ApiResponse.success("Tenant migration preview retrieved",
                service.previewTenant(tenantId, targetVersion)));
    }

    @PostMapping("/tenants/{tenantId}/validate")
    public ResponseEntity<ApiResponse<MigrationPreviewResponse>> validateTenant(
            @PathVariable Long tenantId, @RequestParam(required = false) String targetVersion) {
        return ResponseEntity.ok(ApiResponse.success("Tenant migration validation completed",
                service.previewTenant(tenantId, targetVersion)));
    }

    @PostMapping("/tenants/{tenantId}/migrate")
    public ResponseEntity<ApiResponse<TenantMigrationResponse>> migrate(
            @PathVariable Long tenantId, @RequestParam(required = false) String targetVersion) {
        return ResponseEntity.ok(ApiResponse.success("Tenant migration finished",
                service.migrateTenant(tenantId, targetVersion)));
    }

    @PostMapping("/tenants/{tenantId}/retry-migration")
    public ResponseEntity<ApiResponse<MigrationExecutionResponse>> retryMigration(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Migration retry finished",
                service.retryTenantMigration(tenantId)));
    }

    @PostMapping("/tenants/{tenantId}/retry-catalog")
    public ResponseEntity<ApiResponse<CatalogSyncExecutionResponse>> retryCatalog(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Catalog retry finished",
                service.retryCatalogSync(tenantId)));
    }

    @PutMapping("/tenants/{tenantId}/maintenance")
    public ResponseEntity<ApiResponse<TenantReleaseOperationResponse>> maintenance(
            @PathVariable Long tenantId, @Valid @RequestBody MaintenanceModeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Maintenance state updated",
                service.maintenance(tenantId, request)));
    }

    /* ------------------------------------------------------------------ scripts + catalog */

    @GetMapping("/scripts")
    public ResponseEntity<ApiResponse<List<MigrationScriptResponse>>> scripts() {
        return ResponseEntity.ok(ApiResponse.success("Migration scripts retrieved", service.scripts()));
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<CatalogOverviewResponse>> catalog() {
        return ResponseEntity.ok(ApiResponse.success("Catalog synchronization state retrieved",
                service.catalog()));
    }

    /* ------------------------------------------------------------------ jobs + history */

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<Page<ExecutionJobResponse>>> jobs(
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Execution jobs retrieved",
                service.jobs(jobType, status, search, pageable)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<AuditEventResponse>>> history(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Migration history retrieved",
                service.history(eventType, search, pageable)));
    }
}
