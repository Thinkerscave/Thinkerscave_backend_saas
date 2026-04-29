package com.thinkerscave.common.orgm.controller;

import com.thinkerscave.common.orgm.dto.*;
import com.thinkerscave.common.orgm.service.OrganizationService;
import com.thinkerscave.common.commonModel.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organization Management", description = "APIs related to organization registration and management")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrgResponseDTO>> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody OrgUpdateDTO organizationUpdateDto) {
        log.info("Updating Organization ID: {}", id);
        OrgResponseDTO response = organizationService.updateOrganization(id, organizationUpdateDto);
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", response));
    }

    /**
     * GET /api/organizations/groups
     * * Retrieves a list of organizations that are marked as groups, to be used
     * as potential parent organizations in a dropdown.
     *
     * @return A ResponseEntity containing the list of parent organizations.
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<ParentOrgDTO>>> getParentOrganizations() {
        List<ParentOrgDTO> parentOrgs = organizationService.getParentOrganizations();
        return ResponseEntity.ok(ApiResponse.success("Parent organizations retrieved", parentOrgs));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/all")
    @Operation(summary = "Get all organizations")
    public ResponseEntity<ApiResponse<List<OrganisationListDTO>>> getAllOrganizations() {
        List<OrganisationListDTO> organizations = organizationService.getAllOrgsAsDTO();
        return ResponseEntity.ok(ApiResponse.success("All organizations retrieved", organizations));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{orgCode}")
    public ResponseEntity<ApiResponse<String>> toggleOrganizationStatus(@PathVariable String orgCode) {
        String resultMessage = organizationService.softDeleteOrg(orgCode);
        return ResponseEntity.ok(ApiResponse.success(resultMessage, null));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/owner/update")
    public ResponseEntity<ApiResponse<Void>> updateOwnerDetails(@Valid @RequestBody OwnerDTO dto) {
        organizationService.updateOwnerDetailsWithUser(dto);
        return ResponseEntity.ok(ApiResponse.success("Owner and user details updated successfully.", null));
    }

}
