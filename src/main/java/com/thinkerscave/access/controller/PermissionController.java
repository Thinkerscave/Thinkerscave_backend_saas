package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.UpdateUserPermissionsRequest;
import com.thinkerscave.access.dto.response.EffectivePermissionResponse;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "User-level permission overrides and effective permission checks")
public class PermissionController {

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    @GetMapping("/users/{userId}/permissions")
    @Operation(summary = "Get effective permissions for a user (role + override merged)")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<List<EffectivePermissionResponse>>> getEffectivePermissions(
            @PathVariable Long userId,
            @RequestParam Long organizationId) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getEffectivePermissions(userId, organizationId)));
    }

    @PutMapping("/users/{userId}/permissions")
    @Operation(summary = "Update user-level permission overrides (full replace)")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> updateUserPermissions(
            @PathVariable Long userId,
            @RequestParam Long organizationId,
            @Valid @RequestBody UpdateUserPermissionsRequest request) {
        permissionService.updateUserPermissions(userId, organizationId, request);
        return ResponseEntity.ok(ApiResponse.noContent("User permissions updated"));
    }

    @GetMapping("/users/{userId}/permissions/check")
    @Operation(summary = "Check a user's permission on a specific menu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EffectivePermissionResponse>> checkPermission(
            @PathVariable Long userId,
            @RequestParam Long organizationId,
            @RequestParam Long menuId) {
        assertCanViewPermissions(userId, organizationId);
        return ResponseEntity.ok(ApiResponse.success(permissionService.checkPermission(userId, organizationId, menuId)));
    }

    /**
     * IDOR guard: a caller may only check their own permissions unless they hold an
     * org-admin-level authority, in which case they may still only check users within
     * their own organization.
     */
    private void assertCanViewPermissions(Long requestedUserId, Long requestedOrganizationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        User caller = userRepository.findByUsername(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new AccessDeniedException("Caller not resolvable"));

        boolean isSelf = caller.getId().equals(requestedUserId)
                && caller.getOrganizationId() != null
                && caller.getOrganizationId().equals(requestedOrganizationId);
        boolean isOrgAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ORGANIZATION_ADMIN") || a.getAuthority().equals("ORGANIZATION_OWNER")
                        || a.getAuthority().equals("SUPER_ADMIN"));

        if (isSelf) {
            return;
        }
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SUPER_ADMIN"));
        if (isSuperAdmin) {
            return;
        }
        if (isOrgAdmin && caller.getOrganizationId() != null && caller.getOrganizationId().equals(requestedOrganizationId)) {
            return;
        }
        throw new AccessDeniedException("Not authorized to view this user's permissions");
    }
}
