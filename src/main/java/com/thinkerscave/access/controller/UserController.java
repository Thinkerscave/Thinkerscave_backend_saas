package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.*;
import com.thinkerscave.access.dto.response.*;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.service.UserManagementService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access/organizations/{organizationId}/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD, status and role management")
public class UserController {

    private final UserManagementService userManagementService;

    @PostMapping
    @Operation(summary = "Create a new user in the organization")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> createUser(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User created successfully", userManagementService.createUser(organizationId, request)));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user profile")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUser(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userManagementService.updateUser(organizationId, userId, request)));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUser(
            @PathVariable Long organizationId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getUserById(organizationId, userId)));
    }

    @GetMapping
    @Operation(summary = "Search and list users with pagination")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> searchUsers(
            @PathVariable Long organizationId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) UserStatus status,
            @Parameter(description = "Filter by role type") @RequestParam(required = false) RoleType roleType,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdOn,desc") String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(userManagementService.searchUsers(organizationId, status, roleType, search, pageable)));
    }

    @PatchMapping("/{userId}/activate")
    @Operation(summary = "Activate user")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long organizationId, @PathVariable Long userId) {
        userManagementService.activateUser(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("User activated"));
    }

    @PatchMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate user")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long organizationId, @PathVariable Long userId) {
        userManagementService.deactivateUser(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("User deactivated"));
    }

    @PatchMapping("/{userId}/lock")
    @Operation(summary = "Lock user account")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> lock(@PathVariable Long organizationId, @PathVariable Long userId) {
        userManagementService.lockUser(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("User locked"));
    }

    @PatchMapping("/{userId}/unlock")
    @Operation(summary = "Unlock user account")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> unlock(@PathVariable Long organizationId, @PathVariable Long userId) {
        userManagementService.unlockUser(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("User unlocked"));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Admin-triggered password reset")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long organizationId, @PathVariable Long userId) {
        userManagementService.resetPassword(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("Password reset email sent"));
    }

    @PostMapping("/{userId}/change-password")
    @Operation(summary = "Self-service password change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userManagementService.changePassword(organizationId, userId, request);
        return ResponseEntity.ok(ApiResponse.noContent("Password changed successfully"));
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Assign role to user")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        userManagementService.assignRole(organizationId, userId, roleId);
        return ResponseEntity.ok(ApiResponse.noContent("Role assigned"));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Remove role from user")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        userManagementService.removeRole(organizationId, userId, roleId);
        return ResponseEntity.ok(ApiResponse.noContent("Role removed"));
    }

    @PatchMapping("/{userId}/roles/{roleId}/primary")
    @Operation(summary = "Set primary role for user")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> setPrimaryRole(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        userManagementService.setPrimaryRole(organizationId, userId, roleId);
        return ResponseEntity.ok(ApiResponse.noContent("Primary role updated"));
    }

    @PostMapping("/bulk-status")
    @Operation(summary = "Bulk update user status")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateStatus(
            @PathVariable Long organizationId,
            @Valid @RequestBody BulkUserStatusRequest request) {
        userManagementService.bulkUpdateStatus(organizationId, request);
        return ResponseEntity.ok(ApiResponse.noContent("Bulk status updated"));
    }

    @GetMapping("/{userId}/effective-permissions")
    @Operation(summary = "Get effective permissions for a user (role + user overrides merged)")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<List<EffectivePermissionResponse>>> getEffectivePermissions(
            @PathVariable Long organizationId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getEffectivePermissions(organizationId, userId)));
    }

    private Pageable buildPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, Math.min(size, 100), Sort.by(dir, parts[0]));
    }
}
