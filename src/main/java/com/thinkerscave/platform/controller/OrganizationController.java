package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.OrganizationRequest;
import com.thinkerscave.platform.dto.response.OrganizationDetailResponse;
import com.thinkerscave.platform.dto.response.OrganizationSummaryResponse;
import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.enums.OrganizationStatus;
import com.thinkerscave.platform.service.OrganizationService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/organizations")
@RequiredArgsConstructor
@Tag(name = "Organization Management", description = "Manage organizations (tenants)")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List organizations with filters and pagination")
    public ResponseEntity<ApiResponse<Page<OrganizationSummaryResponse>>> getOrganizations(
            @RequestParam(required = false) OrganizationStatus status,
            @RequestParam(required = false) InstitutionType institutionType,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Organizations retrieved",
                organizationService.getOrganizations(status, institutionType, customerId, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get organization 360 detail")
    public ResponseEntity<ApiResponse<OrganizationDetailResponse>> getOrganizationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Organization detail retrieved", organizationService.getOrganizationById(id)));
    }

    @GetMapping("/{id}/invoice")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Download onboarding invoice PDF generated from the subscription snapshot")
    public ResponseEntity<byte[]> downloadOnboardingInvoice(@PathVariable Long id) {
        byte[] pdf = organizationService.downloadOnboardingInvoicePdf(id);
        String filename = "onboarding-invoice-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create an organization (without provisioning)")
    public ResponseEntity<ApiResponse<OrganizationSummaryResponse>> createOrganization(
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Organization created successfully", organizationService.createOrganization(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update organization")
    public ResponseEntity<ApiResponse<OrganizationSummaryResponse>> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", organizationService.updateOrganization(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Archive organization")
    public ResponseEntity<ApiResponse<Void>> archiveOrganization(@PathVariable Long id) {
        organizationService.archiveOrganization(id);
        return ResponseEntity.ok(ApiResponse.noContent("Organization archived successfully"));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Activate organization")
    public ResponseEntity<ApiResponse<OrganizationSummaryResponse>> activateOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Organization activated", organizationService.activateOrganization(id)));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Suspend organization")
    public ResponseEntity<ApiResponse<OrganizationSummaryResponse>> suspendOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Organization suspended", organizationService.suspendOrganization(id)));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Archive organization via POST")
    public ResponseEntity<ApiResponse<Void>> archiveOrganizationPost(@PathVariable Long id) {
        organizationService.archiveOrganization(id);
        return ResponseEntity.ok(ApiResponse.noContent("Organization archived successfully"));
    }
}
