package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.staff.dto.request.BulkResponsibilityAssignmentRequest;
import com.thinkerscave.staff.dto.request.ResponsibilityRequest;
import com.thinkerscave.staff.dto.response.ResponsibilityAssignmentResponse;
import com.thinkerscave.staff.dto.response.ResponsibilityResponse;
import com.thinkerscave.staff.service.ResponsibilityAssignmentService;
import com.thinkerscave.staff.service.ResponsibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/responsibilities")
@RequiredArgsConstructor
@Tag(name = "Responsibility Management", description = "APIs for managing staff responsibilities")
public class ResponsibilityController {

    private final ResponsibilityService responsibilityService;
    private final ResponsibilityAssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create a new responsibility")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createResponsibility(
            @Valid @RequestBody ResponsibilityRequest request) {
        Long id = responsibilityService.createResponsibility(request);
        return ResponseEntity.status(201).body(
                ApiResponse.created("Responsibility created successfully", Map.of("responsibilityId", id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update responsibility")
    public ResponseEntity<ApiResponse<Void>> updateResponsibility(
            @PathVariable Long id,
            @Valid @RequestBody ResponsibilityRequest request) {
        responsibilityService.updateResponsibility(id, request);
        return ResponseEntity.ok(ApiResponse.noContent("Responsibility updated successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get responsibilities")
    public ResponseEntity<ApiResponse<List<ResponsibilityResponse>>> getResponsibilityList(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success("Responsibilities retrieved",
                responsibilityService.getResponsibilityList(includeInactive)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get responsibility by ID")
    public ResponseEntity<ApiResponse<ResponsibilityResponse>> getResponsibilityById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Responsibility retrieved",
                responsibilityService.getResponsibilityById(id)));
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Staff currently assigned to this responsibility")
    public ResponseEntity<ApiResponse<List<ResponsibilityAssignmentResponse>>> getAssignedStaff(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Assigned staff retrieved",
                assignmentService.getResponsibilityStaff(id)));
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Assign this responsibility to one or more staff")
    public ResponseEntity<ApiResponse<Void>> assignStaff(
            @PathVariable Long id,
            @Valid @RequestBody BulkResponsibilityAssignmentRequest request) {
        assignmentService.assignStaff(id, request);
        return ResponseEntity.ok(ApiResponse.noContent("Staff assigned to responsibility"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Activate a responsibility")
    public ResponseEntity<ApiResponse<Void>> activateResponsibility(@PathVariable Long id) {
        responsibilityService.activateResponsibility(id);
        return ResponseEntity.ok(ApiResponse.noContent("Responsibility activated"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate a responsibility")
    public ResponseEntity<ApiResponse<Void>> deactivateResponsibility(@PathVariable Long id) {
        responsibilityService.deactivateResponsibility(id);
        return ResponseEntity.ok(ApiResponse.noContent("Responsibility deactivated"));
    }
}
