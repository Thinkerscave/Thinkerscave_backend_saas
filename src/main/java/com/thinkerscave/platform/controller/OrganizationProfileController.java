package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.OrganizationProfileUpdateRequest;
import com.thinkerscave.platform.dto.response.OrganizationProfileResponse;
import com.thinkerscave.platform.service.OrganizationService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.exceptions.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service Organization Profile for Organization Admin/Owner — always scoped to the
 * caller's own {@link OrganizationContext}, never to a path/body-supplied organization id.
 */
@RestController
@RequestMapping("/api/v1/organization-profile")
@RequiredArgsConstructor
@Tag(name = "Organization Profile", description = "Self-service organization profile for Organization Admin/Owner")
public class OrganizationProfileController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get my organization's profile")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success("Organization profile retrieved",
                organizationService.getMyOrganizationProfile(requireOrganizationId())));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update my organization's profile (Institution Type is not editable here)")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>> updateMyProfile(
            @Valid @RequestBody OrganizationProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Organization profile updated",
                organizationService.updateMyOrganizationProfile(requireOrganizationId(), request)));
    }

    private Long requireOrganizationId() {
        Long organizationId = OrganizationContext.getOrganizationId();
        if (organizationId == null || organizationId <= 0) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationId;
    }
}
