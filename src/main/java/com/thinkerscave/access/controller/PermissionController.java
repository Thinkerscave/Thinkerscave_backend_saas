package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.UpdateUserPermissionsRequest;
import com.thinkerscave.access.dto.response.EffectivePermissionResponse;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "User-level permission overrides and effective permission checks")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/users/{userId}/permissions")
    @Operation(summary = "Get effective permissions for a user (role + override merged)")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<List<EffectivePermissionResponse>>> getEffectivePermissions(
            @PathVariable Long userId,
            @RequestParam Long organizationId) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getEffectivePermissions(userId, organizationId)));
    }

    @PutMapping("/users/{userId}/permissions")
    @Operation(summary = "Update user-level permission overrides (full replace)")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> updateUserPermissions(
            @PathVariable Long userId,
            @RequestParam Long organizationId,
            @Valid @RequestBody UpdateUserPermissionsRequest request) {
        permissionService.updateUserPermissions(userId, organizationId, request);
        return ResponseEntity.ok(ApiResponse.noContent("User permissions updated"));
    }

    @GetMapping("/users/{userId}/permissions/check")
    @Operation(summary = "Check a user's permission on a specific menu")
    public ResponseEntity<ApiResponse<EffectivePermissionResponse>> checkPermission(
            @PathVariable Long userId,
            @RequestParam Long organizationId,
            @RequestParam Long menuId) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.checkPermission(userId, organizationId, menuId)));
    }
}
