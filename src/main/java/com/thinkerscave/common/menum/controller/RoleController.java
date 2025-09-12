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
@CrossOrigin("http://localhost:4200/")
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<RoleDTO> saveOrUpdateRole(@RequestBody RoleDTO dto) {
        log.info("API Request - Save/Update Role");
        return ResponseEntity.ok(roleService.saveOrUpdateRole(dto));
    }

    @GetMapping
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        log.info("API Request - Get All Roles");
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{roleCode}")
    public ResponseEntity<RoleDTO> getRoleByCode(@PathVariable String roleCode) {
        log.info("API Request - Get Role By Code: {}", roleCode);
        return ResponseEntity.ok(roleService.getRoleByCode(roleCode));
    }

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
    
    @GetMapping("/active")
    public ResponseEntity<List<RoleLookupDTO>> getActiveRoles() {
        log.info("API Request - Get Active Roles");
        return ResponseEntity.ok(roleService.getActiveRoles());
    }
}

