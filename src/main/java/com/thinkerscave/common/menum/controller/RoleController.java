package com.thinkerscave.common.menum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.thinkerscave.common.menum.dto.RoleDTO;
import com.thinkerscave.common.menum.dto.RoleLookupDTO;
import com.thinkerscave.common.menum.service.RoleService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Save or update role", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @PostMapping
    public ResponseEntity<RoleDTO> saveOrUpdateRole(@RequestBody RoleDTO dto) {
        log.info("API Request - Save/Update Role");
        return ResponseEntity.ok(roleService.saveOrUpdateRole(dto));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get all roles", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        log.info("API Request - Get All Roles");
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get role by code", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping("/{roleCode}")
    public ResponseEntity<RoleDTO> getRoleByCode(@PathVariable String roleCode) {
        log.info("API Request - Get Role By Code: {}", roleCode);
        return ResponseEntity.ok(roleService.getRoleByCode(roleCode));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Update role status", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @PatchMapping("/updateStatus")
    public ResponseEntity<Map<String, String>> updateRoleStatus(
            @RequestParam(required = false) Long roleId,
            @RequestParam Boolean status) {
        log.info("API Request - Update Role Status");
        roleService.updateRoleStatus(roleId, status);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Role status updated successfully ✅");

        return ResponseEntity.ok(response);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get active roles", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping("/active")
    public ResponseEntity<List<RoleLookupDTO>> getActiveRoles() {
        log.info("API Request - Get Active Roles");
        return ResponseEntity.ok(roleService.getActiveRoles());
    }
}
