package com.thinkerscave.common.menum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thinkerscave.common.menum.dto.MenuMappingDTO;
import com.thinkerscave.common.menum.dto.RoleMenuMappingRequest;
import com.thinkerscave.common.menum.dto.SideMenuDTO;
import com.thinkerscave.common.menum.service.MenuMappingService;
import com.thinkerscave.common.security.UserInfoUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/menu-mapping")
@RequiredArgsConstructor
public class MenuMappingController {

    private final MenuMappingService menuMappingService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Get role-based side menu", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping
    public ResponseEntity<List<SideMenuDTO>> getRoleBasedSideMenu(
            @AuthenticationPrincipal UserInfoUserDetails userInfoUserDetails) {
        Long roleId = userInfoUserDetails.getRoleId();
        List<SideMenuDTO> sideMenuList = menuMappingService.getRoleBasedSideMenu(roleId);
        return sideMenuList.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(sideMenuList);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get active menu tree", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping("/getActiveMenuTree")
    public ResponseEntity<List<MenuMappingDTO>> getActiveMenuTree() {
        List<MenuMappingDTO> result = menuMappingService.getActiveMenuTree();
        return result.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(result);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Assign role menu privileges", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @PostMapping("/assign")
    public ResponseEntity<String> assignRoleMenuPrivileges(
            @RequestBody RoleMenuMappingRequest request) {
        menuMappingService.assignRoleMenuPrivileges(request);
        return ResponseEntity.ok("Role menu privileges assigned successfully!");
    }

}
