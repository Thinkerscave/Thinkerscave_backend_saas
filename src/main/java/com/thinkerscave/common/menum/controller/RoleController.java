package com.thinkerscave.common.menum.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.thinkerscave.common.menum.dto.RoleDTO;
import com.thinkerscave.common.menum.dto.RoleLookupDTO;
import com.thinkerscave.common.menum.service.RoleService;
import com.thinkerscave.common.commonModel.ApiResponse;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Role Management", description = "APIs for defining and managing system and tenant-specific roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Save or update role")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleDTO>> saveOrUpdateRole(@Valid @RequestBody RoleDTO dto) {
        log.info("API Request - Save/Update Role: {}", dto.getRoleName());
        RoleDTO savedRole = roleService.saveOrUpdateRole(dto);
        return ResponseEntity.ok(ApiResponse.success("Role saved successfully", savedRole));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get all roles")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles() {
        log.info("API Request - Get All Roles");
        List<RoleDTO> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get role by code")
    @GetMapping("/{roleCode}")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleByCode(@PathVariable String roleCode) {
        log.info("API Request - Get Role By Code: {}", roleCode);
        RoleDTO role = roleService.getRoleByCode(roleCode);
        return ResponseEntity.ok(ApiResponse.success("Role found", role));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Update role status")
    @PatchMapping("/updateStatus")
    public ResponseEntity<ApiResponse<Void>> updateRoleStatus(
            @RequestParam(required = false) Long roleId,
            @RequestParam Boolean status) {
        log.info("API Request - Update Role Status for ID: {}", roleId);
        roleService.updateRoleStatus(roleId, status);
        return ResponseEntity.ok(ApiResponse.success("Role status updated successfully", null));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get active roles")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<RoleLookupDTO>>> getActiveRoles() {
        log.info("API Request - Get Active Roles");
        List<RoleLookupDTO> roles = roleService.getActiveRoles();
        return ResponseEntity.ok(ApiResponse.success("Active roles retrieved", roles));
    }
}
