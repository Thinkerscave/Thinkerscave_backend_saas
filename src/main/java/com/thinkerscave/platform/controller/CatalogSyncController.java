package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.CatalogSyncExecutionResponse;
import com.thinkerscave.platform.dto.response.CatalogSyncStatusResponse;
import com.thinkerscave.platform.service.TenantCatalogSyncService;
import com.thinkerscave.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/catalog-sync")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class CatalogSyncController {
    private final TenantCatalogSyncService catalogSyncService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<CatalogSyncStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success("Catalog sync status retrieved",
                catalogSyncService.status()));
    }

    @GetMapping("/tenants/{tenantId}/history")
    public ResponseEntity<ApiResponse<List<CatalogSyncExecutionResponse>>> history(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Catalog sync history retrieved",
                catalogSyncService.historyForTenant(tenantId)));
    }

    @PostMapping("/tenants/{tenantId}/retry")
    public ResponseEntity<ApiResponse<CatalogSyncExecutionResponse>> retry(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Catalog sync retried",
                catalogSyncService.retryLatestForTenant(tenantId)));
    }
}
