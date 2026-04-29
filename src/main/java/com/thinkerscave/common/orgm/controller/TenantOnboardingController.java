package com.thinkerscave.common.orgm.controller;

import com.thinkerscave.common.orgm.dto.TenantOnboardingRequest;
import com.thinkerscave.common.orgm.dto.TenantOnboardingResponse;
import com.thinkerscave.common.orgm.dto.TenantStatusResponse;
import com.thinkerscave.common.orgm.service.TenantOnboardingService;
import com.thinkerscave.common.commonModel.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Secure tenant onboarding controller.
 * All endpoints require SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/tenant-onboarding")
@Tag(name = "Tenant Onboarding (PRODUCTION)", description = "Production-ready tenant provisioning and lifecycle management. "
        +
        "Handles complete onboarding workflow including schema creation, user seeding, configuration, and audit logging.")
@RequiredArgsConstructor
@Slf4j
public class TenantOnboardingController {

    private final TenantOnboardingService onboardingService;

    @PostMapping("/provision")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Provision new tenant", description = "Creates complete tenant: schema, admin user, configuration, subdomain", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tenant created successfully", content = @Content(schema = @Schema(implementation = TenantOnboardingResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - requires SUPER_ADMIN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tenant already exists")
    })
    public ResponseEntity<ApiResponse<TenantOnboardingResponse>> provisionTenant(
            @RequestBody @Valid TenantOnboardingRequest request,
            HttpServletRequest httpRequest) {

        // Inject audit fields
        request.setIpAddress(httpRequest.getRemoteAddr());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            request.setPerformedBy(auth.getName());
        }

        log.info("Provisioning tenant: {} by {}", request.getTenantName(), request.getPerformedBy());
        TenantOnboardingResponse response = onboardingService.onboardNewTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tenant provisioned successfully", response));
    }

    @GetMapping("/status/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get tenant status", description = "Returns current status and resource usage of a tenant", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<TenantStatusResponse>> getTenantStatus(@PathVariable String tenantId) {
        TenantStatusResponse status = onboardingService.getTenantStatus(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Tenant status retrieved", status));
    }

    @PostMapping("/{tenantId}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate tenant", description = "Enables a deactivated tenant", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> activateTenant(@PathVariable String tenantId) {
        onboardingService.activateTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Tenant activated successfully", null));
    }

    @PostMapping("/{tenantId}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate tenant", description = "Disables a tenant without deleting data", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> deactivateTenant(@PathVariable String tenantId) {
        onboardingService.deactivateTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Tenant deactivated successfully", null));
    }

}
