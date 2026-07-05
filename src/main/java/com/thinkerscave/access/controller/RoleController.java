package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.*;
import com.thinkerscave.access.dto.response.*;
import com.thinkerscave.access.service.RoleService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/access/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Role CRUD and permission matrix management")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @Operation(summary = "Create a new role")
    @PreAuthorize("hasAuthority('ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Role created", roleService.createRole(request)));
    }

    @PutMapping("/{roleId}")
    @Operation(summary = "Update a role")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Role updated", roleService.updateRole(roleId, request)));
    }

    @GetMapping("/{roleId}")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable Long roleId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getRoleById(roleId)));
    }

    @GetMapping
    @Operation(summary = "List all active roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllActiveRoles()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search roles with pagination")
    public ResponseEntity<ApiResponse<Page<RoleResponse>>> searchRoles(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder").ascending());
        return ResponseEntity.ok(ApiResponse.success(roleService.searchRoles(active, search, pageable)));
    }

    @PatchMapping("/{roleId}/activate")
    @Operation(summary = "Activate a role")
    @PreAuthorize("hasAuthority('ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long roleId) {
        roleService.activateRole(roleId);
        return ResponseEntity.ok(ApiResponse.noContent("Role activated"));
    }

    @PatchMapping("/{roleId}/deactivate")
    @Operation(summary = "Deactivate a role")
    @PreAuthorize("hasAuthority('ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long roleId) {
        roleService.deactivateRole(roleId);
        return ResponseEntity.ok(ApiResponse.noContent("Role deactivated"));
    }

    @GetMapping("/{roleId}/permissions")
    @Operation(summary = "Get permission matrix for a role scoped to an organization")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<PermissionMatrixResponse>> getPermissionMatrix(
            @PathVariable Long roleId,
            @RequestParam Long organizationId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getPermissionMatrix(roleId, organizationId)));
    }

    @PutMapping("/{roleId}/permissions")
    @Operation(summary = "Update (replace) permission matrix for a role")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> updatePermissionMatrix(
            @PathVariable Long roleId,
            @RequestParam Long organizationId,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        roleService.updatePermissionMatrix(roleId, organizationId, request);
        return ResponseEntity.ok(ApiResponse.noContent("Permission matrix updated"));
    }
}
