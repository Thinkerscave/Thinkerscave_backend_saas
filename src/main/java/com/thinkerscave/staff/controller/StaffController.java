package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.staff.dto.request.StaffCreateRequest;
import com.thinkerscave.staff.dto.request.StaffUpdateRequest;
import com.thinkerscave.staff.dto.response.StaffCreateResponse;
import com.thinkerscave.staff.dto.response.StaffDashboardResponse;
import com.thinkerscave.staff.dto.response.StaffDetailResponse;
import com.thinkerscave.staff.dto.response.StaffSummaryResponse;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import com.thinkerscave.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Management", description = "APIs for managing staff members")
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create a new staff member")
    public ResponseEntity<ApiResponse<StaffCreateResponse>> createStaff(
            @Valid @RequestBody StaffCreateRequest request) {
        StaffCreateResponse response = staffService.createStaff(request);
        return ResponseEntity.status(201).body(ApiResponse.created("Staff created successfully", response));
    }

    @PutMapping("/{staffId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update staff details")
    public ResponseEntity<ApiResponse<Void>> updateStaff(
            @PathVariable Long staffId,
            @Valid @RequestBody StaffUpdateRequest request) {
        staffService.updateStaff(staffId, request);
        return ResponseEntity.ok(ApiResponse.noContent("Staff updated successfully"));
    }

    @GetMapping("/{staffId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get staff 360 profile details")
    public ResponseEntity<ApiResponse<StaffDetailResponse>> getStaffDetail(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Staff details retrieved", staffService.getStaffDetail(staffId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get paginated staff list with filters")
    public ResponseEntity<ApiResponse<Page<StaffSummaryResponse>>> getStaffList(
            @RequestParam(required = false) StaffType staffType,
            @RequestParam(required = false) EmploymentCategory employmentCategory,
            @RequestParam(required = false) EmploymentStatus employmentStatus,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable) {
        Page<StaffSummaryResponse> page = staffService.getStaffList(
                staffType, employmentCategory, employmentStatus, designation, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Staff list retrieved", page));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get staff dashboard KPI metrics")
    public ResponseEntity<ApiResponse<StaffDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved", staffService.getDashboard()));
    }

    @PatchMapping("/{staffId}/activate")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Activate a staff member")
    public ResponseEntity<ApiResponse<Void>> activateStaff(@PathVariable Long staffId) {
        staffService.activateStaff(staffId);
        return ResponseEntity.ok(ApiResponse.noContent("Staff activated successfully"));
    }

    @PatchMapping("/{staffId}/deactivate")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate a staff member")
    public ResponseEntity<ApiResponse<Void>> deactivateStaff(@PathVariable Long staffId) {
        staffService.deactivateStaff(staffId);
        return ResponseEntity.ok(ApiResponse.noContent("Staff deactivated successfully"));
    }

    @DeleteMapping("/{staffId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Soft delete a staff member")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable Long staffId) {
        staffService.deleteStaff(staffId);
        return ResponseEntity.ok(ApiResponse.noContent("Staff deleted successfully"));
    }
}
