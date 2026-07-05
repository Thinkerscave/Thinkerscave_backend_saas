package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.TenantConfigResponse;
import com.thinkerscave.platform.service.TenantSettingsService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant-settings")
@RequiredArgsConstructor
@Tag(name = "Tenant Settings", description = "Organization-scoped UI labels and academic structure hints")
public class TenantSettingsController {

    private final TenantSettingsService tenantSettingsService;

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get tenant UI configuration for the current organization")
    public ResponseEntity<ApiResponse<TenantConfigResponse>> getCurrentTenantConfig() {
        return ResponseEntity.ok(ApiResponse.success(
                "Tenant configuration retrieved",
                tenantSettingsService.getCurrentTenantConfig()));
    }
}
