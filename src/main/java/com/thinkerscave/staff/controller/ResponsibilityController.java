package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
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

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL')")
    @Operation(summary = "Create a new responsibility")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createResponsibility(
            @Valid @RequestBody ResponsibilityRequest request) {
        Long id = responsibilityService.createResponsibility(request);
        return ResponseEntity.status(201).body(
                ApiResponse.created("Responsibility created successfully", Map.of("responsibilityId", id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL')")
    @Operation(summary = "Update responsibility")
    public ResponseEntity<ApiResponse<Void>> updateResponsibility(
            @PathVariable Long id,
            @Valid @RequestBody ResponsibilityRequest request) {
        responsibilityService.updateResponsibility(id, request);
        return ResponseEntity.ok(ApiResponse.noContent("Responsibility updated successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL')")
    @Operation(summary = "Get all active responsibilities")
    public ResponseEntity<ApiResponse<List<ResponsibilityResponse>>> getResponsibilityList() {
        return ResponseEntity.ok(ApiResponse.success("Responsibilities retrieved",
                responsibilityService.getResponsibilityList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL')")
    @Operation(summary = "Get responsibility by ID")
    public ResponseEntity<ApiResponse<ResponsibilityResponse>> getResponsibilityById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Responsibility retrieved",
                responsibilityService.getResponsibilityById(id)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL')")
    @Operation(summary = "Activate a responsibility")
    public ResponseEntity<ApiResponse<Void>> activateResponsibility(@PathVariable Long id) {
        responsibilityService.activateResponsibility(id);
        return ResponseEntity.ok(ApiResponse.noContent("Responsibility activated"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL')")
    @Operation(summary = "Deactivate a responsibility")
    public ResponseEntity<ApiResponse<Void>> deactivateResponsibility(@PathVariable Long id) {
        responsibilityService.deactivateResponsibility(id);
        return ResponseEntity.ok(ApiResponse.noContent("Responsibility deactivated"));
    }
}
