package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.staff.dto.request.StaffProfileUpdateRequest;
import com.thinkerscave.staff.dto.response.PayrollResponse;
import com.thinkerscave.staff.dto.response.ResponsibilityAssignmentResponse;
import com.thinkerscave.staff.dto.response.StaffDetailResponse;
import com.thinkerscave.staff.service.StaffProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/me")
@RequiredArgsConstructor
@Tag(name = "Staff Self-Service", description = "APIs for staff to manage their own profile")
public class StaffProfileController {

    private final StaffProfileService staffProfileService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get logged-in staff profile")
    public ResponseEntity<ApiResponse<StaffDetailResponse>> getMyProfile() {
        String username = currentUsername();
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved",
                staffProfileService.getMyProfile(username)));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update logged-in staff profile (limited fields)")
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @Valid @RequestBody StaffProfileUpdateRequest request) {
        String username = currentUsername();
        staffProfileService.updateMyProfile(username, request);
        return ResponseEntity.ok(ApiResponse.noContent("Profile updated successfully"));
    }

    @GetMapping("/responsibilities")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get responsibilities for logged-in staff")
    public ResponseEntity<ApiResponse<List<ResponsibilityAssignmentResponse>>> getMyResponsibilities() {
        String username = currentUsername();
        return ResponseEntity.ok(ApiResponse.success("Responsibilities retrieved",
                staffProfileService.getMyResponsibilities(username)));
    }

    @GetMapping("/payroll")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payroll history for logged-in staff")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> getMyPayrollHistory() {
        String username = currentUsername();
        return ResponseEntity.ok(ApiResponse.success("Payroll history retrieved",
                staffProfileService.getMyPayrollHistory(username)));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return authentication.getName();
    }
}
