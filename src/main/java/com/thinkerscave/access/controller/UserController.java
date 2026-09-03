package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.*;
import com.thinkerscave.access.dto.response.*;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.UserManagementService;
import com.thinkerscave.security.service.impl.PublicSchemaUserLookupService;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.util.PageRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/access/organizations/{organizationId}/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD, status and role management")
public class UserController {

    private final UserManagementService userManagementService;
    private final UserRepository userRepository;
    private final PublicSchemaUserLookupService publicSchemaUserLookupService;

    @PostMapping
    @Operation(summary = "Create a new user in the organization")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> createUser(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User created successfully", userManagementService.createUser(organizationId, request)));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user profile")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUser(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userManagementService.updateUser(organizationId, userId, request)));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUser(
            @PathVariable Long organizationId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getUserById(organizationId, userId)));
    }

    @GetMapping
    @Operation(summary = "Search and list users with pagination")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> searchUsers(
            @PathVariable Long organizationId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) UserStatus status,
            @Parameter(description = "Filter by role type") @RequestParam(required = false) RoleType roleType,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn,desc") String sort) {
        Pageable pageable = PageRequestUtil.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(userManagementService.searchUsers(organizationId, status, roleType, search, pageable)));
    }

    @PatchMapping("/{userId}/activate")
    @Operation(summary = "Activate user")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long organizationId, @PathVariable Long userId) {
        userManagementService.activateUser(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("User activated"));
    }

    @PatchMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate user")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long organizationId, @PathVariable Long userId) {
        userManagementService.deactivateUser(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("User deactivated"));
    }

    @PatchMapping("/{userId}/lock")
    @Operation(summary = "Lock user account")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> lock(@PathVariable Long organizationId, @PathVariable Long userId) {
        userManagementService.lockUser(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("User locked"));
    }

    @PatchMapping("/{userId}/unlock")
    @Operation(summary = "Unlock user account")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> unlock(@PathVariable Long organizationId, @PathVariable Long userId) {
        userManagementService.unlockUser(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.noContent("User unlocked"));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Admin-triggered password reset")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
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
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        userManagementService.assignRole(organizationId, userId, roleId);
        return ResponseEntity.ok(ApiResponse.noContent("Role assigned"));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Remove role from user")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        userManagementService.removeRole(organizationId, userId, roleId);
        return ResponseEntity.ok(ApiResponse.noContent("Role removed"));
    }

    @PatchMapping("/{userId}/roles/{roleId}/primary")
    @Operation(summary = "Set primary role for user")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> setPrimaryRole(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        userManagementService.setPrimaryRole(organizationId, userId, roleId);
        return ResponseEntity.ok(ApiResponse.noContent("Primary role updated"));
    }

    @PostMapping("/bulk-status")
    @Operation(summary = "Bulk update user status")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateStatus(
            @PathVariable Long organizationId,
            @Valid @RequestBody BulkUserStatusRequest request) {
        userManagementService.bulkUpdateStatus(organizationId, request);
        return ResponseEntity.ok(ApiResponse.noContent("Bulk status updated"));
    }

    @GetMapping("/{userId}/effective-permissions")
    @Operation(summary = "Get effective permissions for a user (role + user overrides merged)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EffectivePermissionResponse>>> getEffectivePermissions(
            @PathVariable Long organizationId,
            @PathVariable Long userId) {
        assertCanViewEffectivePermissions(userId);
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getEffectivePermissions(organizationId, userId)));
    }

    /**
     * IDOR guard: a non-admin caller may only fetch their own effective permissions.
     */
    private void assertCanViewEffectivePermissions(Long requestedUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        if (hasAnyAuthority(authentication, "SUPER_ADMIN", "ORGANIZATION_ADMIN", "ORGANIZATION_OWNER")) {
            return;
        }
        User caller = resolveCaller(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Caller not resolvable"));
        if (!caller.getId().equals(requestedUserId)) {
            throw new AccessDeniedException("Not authorized to view this user's permissions");
        }
    }

    private Optional<User> resolveCaller(String usernameOrEmail) {
        Optional<User> ambient = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));
        if (ambient.isPresent()) {
            return ambient;
        }
        String previousTenant = TenantContext.getTenant();
        try {
            TenantContext.setTenant("public");
            return publicSchemaUserLookupService.findAnyInPublicSchema(usernameOrEmail);
        } finally {
            TenantContext.setTenant(previousTenant);
        }
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        Collection<? extends GrantedAuthority> granted = authentication.getAuthorities();
        for (String authority : authorities) {
            for (GrantedAuthority ga : granted) {
                if (authority.equals(ga.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }
}
