package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.staff.dto.request.ResponsibilityAssignmentRequest;
import com.thinkerscave.staff.dto.response.ResponsibilityAssignmentResponse;
import com.thinkerscave.staff.service.ResponsibilityAssignmentService;
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
@RequiredArgsConstructor
@Tag(name = "Responsibility Assignment", description = "APIs for assigning responsibilities to staff")
public class ResponsibilityAssignmentController {

    private final ResponsibilityAssignmentService assignmentService;

    @PostMapping("/api/v1/staff/responsibility-assignments")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL')")
    @Operation(summary = "Assign a responsibility to a staff member")
    public ResponseEntity<ApiResponse<Map<String, Long>>> assignResponsibility(
            @Valid @RequestBody ResponsibilityAssignmentRequest request) {
        Long id = assignmentService.assignResponsibility(request);
        return ResponseEntity.status(201).body(
                ApiResponse.created("Responsibility assigned", Map.of("assignmentId", id)));
    }

    @PatchMapping("/api/v1/staff/responsibility-assignments/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL')")
    @Operation(summary = "Remove (deactivate) a responsibility assignment")
    public ResponseEntity<ApiResponse<Void>> removeAssignment(@PathVariable Long id) {
        assignmentService.removeAssignment(id);
        return ResponseEntity.ok(ApiResponse.noContent("Assignment removed successfully"));
    }

    @GetMapping("/api/v1/staff/{staffId}/responsibilities")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL')")
    @Operation(summary = "Get all responsibilities for a staff member")
    public ResponseEntity<ApiResponse<List<ResponsibilityAssignmentResponse>>> getStaffResponsibilities(
            @PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Responsibilities retrieved",
                assignmentService.getStaffResponsibilities(staffId)));
    }
}
