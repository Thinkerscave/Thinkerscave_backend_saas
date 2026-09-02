package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.UpdateRolePermissionsRequest;
import com.thinkerscave.access.dto.response.PermissionMatrixResponse;
import com.thinkerscave.access.service.ResponsibilityPermissionService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/access/responsibilities")
@RequiredArgsConstructor
@Tag(name = "Responsibility permissions", description = "Menus and submenus assigned to an organization responsibility")
public class ResponsibilityPermissionController {

    private final ResponsibilityPermissionService permissionService;

    @GetMapping("/{responsibilityId}/permissions")
    @Operation(summary = "Get menu assignment matrix for a responsibility")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<PermissionMatrixResponse>> getPermissionMatrix(
            @PathVariable Long responsibilityId,
            @RequestParam Long organizationId) {
        return ResponseEntity.ok(ApiResponse.success(
                permissionService.getPermissionMatrix(responsibilityId, organizationId)));
    }

    @PutMapping("/{responsibilityId}/permissions")
    @Operation(summary = "Replace menu assignment for a responsibility")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> updatePermissionMatrix(
            @PathVariable Long responsibilityId,
            @RequestParam Long organizationId,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        permissionService.updatePermissionMatrix(responsibilityId, organizationId, request);
        return ResponseEntity.ok(ApiResponse.noContent("Menu assignment updated"));
    }
}
