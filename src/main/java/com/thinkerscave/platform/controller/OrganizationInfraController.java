package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.DomainVerifyRequest;
import com.thinkerscave.platform.dto.request.OrganizationConfigurationRequest;
import com.thinkerscave.platform.dto.request.OrganizationDomainRequest;
import com.thinkerscave.platform.dto.response.OrganizationConfigurationResponse;
import com.thinkerscave.platform.dto.response.OrganizationDomainResponse;
import com.thinkerscave.platform.service.OrganizationInfraService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Organization Infrastructure", description = "Manage organization domains and configurations")
public class OrganizationInfraController {

    private final OrganizationInfraService infraService;

    // ── Domains ───────────────────────────────────────────────────────────────

    @GetMapping("/api/platform/organization-domains")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List all organization domains")
    public ResponseEntity<ApiResponse<List<OrganizationDomainResponse>>> getAllDomains() {
        return ResponseEntity.ok(ApiResponse.success("Domains retrieved", infraService.getAllDomains()));
    }

    @PostMapping("/api/platform/organization-domains")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create domain for organization")
    public ResponseEntity<ApiResponse<OrganizationDomainResponse>> createDomain(
            @Valid @RequestBody OrganizationDomainRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Domain created", infraService.createDomain(request)));
    }

    @PutMapping("/api/platform/organization-domains/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update organization domain")
    public ResponseEntity<ApiResponse<OrganizationDomainResponse>> updateDomain(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationDomainRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Domain updated", infraService.updateDomain(id, request)));
    }

    @PostMapping("/api/platform/organization-domains/verify")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Verify domain DNS configuration")
    public ResponseEntity<ApiResponse<OrganizationDomainResponse>> verifyDomain(
            @Valid @RequestBody DomainVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Domain verified", infraService.verifyDomain(request)));
    }

    @PostMapping("/api/platform/organization-domains/test")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Test domain connectivity")
    public ResponseEntity<ApiResponse<OrganizationDomainResponse>> testDomain(
            @Valid @RequestBody DomainVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Domain test completed", infraService.testDomain(request)));
    }

    // ── Configurations ────────────────────────────────────────────────────────

    @GetMapping("/api/platform/organization-configurations/{organizationId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get organization configuration")
    public ResponseEntity<ApiResponse<OrganizationConfigurationResponse>> getConfiguration(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(ApiResponse.success("Configuration retrieved", infraService.getConfiguration(organizationId)));
    }

    @PutMapping("/api/platform/organization-configurations/{organizationId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update organization configuration")
    public ResponseEntity<ApiResponse<OrganizationConfigurationResponse>> updateConfiguration(
            @PathVariable Long organizationId,
            @Valid @RequestBody OrganizationConfigurationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Configuration updated", infraService.updateConfiguration(organizationId, request)));
    }
}
