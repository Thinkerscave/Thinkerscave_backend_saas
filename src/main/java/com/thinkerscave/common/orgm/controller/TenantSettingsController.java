package com.thinkerscave.common.orgm.controller;

import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.orgm.dto.TenantConfigDTO;
import com.thinkerscave.common.orgm.service.TenantSettingsService;
import com.thinkerscave.common.commonModel.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
@RestController
@RequestMapping("/api/v1/tenant-settings")
@RequiredArgsConstructor
@Tag(name = "Tenant Settings", description = "Endpoints for multi-tenant frontend configuration metadata")
public class TenantSettingsController {
 
    private final TenantSettingsService tenantSettingsService;
 
    @GetMapping("/current")
    @Operation(summary = "Get current tenant configuration", description = "Returns UI metadata specific to the caller's tenant schema")
    public ResponseEntity<ApiResponse<TenantConfigDTO>> getCurrentTenantConfig() {
        String tenantSchema = TenantContext.getTenant();
        TenantConfigDTO config = tenantSettingsService.getTenantConfigBySchema(tenantSchema);
        return ResponseEntity.ok(ApiResponse.success("Tenant configuration retrieved", config));
    }
}
