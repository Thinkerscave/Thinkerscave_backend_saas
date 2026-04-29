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
import com.thinkerscave.common.commonModel.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/menu-mapping")
@Tag(name = "Menu Mapping", description = "APIs for role-based menu configuration and tree management")
@RequiredArgsConstructor
public class MenuMappingController {

        private final MenuMappingService menuMappingService;

        @io.swagger.v3.oas.annotations.Operation(summary = "Get role-based side menu")
        @GetMapping
        public ResponseEntity<ApiResponse<List<SideMenuDTO>>> getRoleBasedSideMenu(
                        @AuthenticationPrincipal UserInfoUserDetails userInfoUserDetails) {
                Long roleId = userInfoUserDetails.getRoleId();
                List<SideMenuDTO> sideMenuList = menuMappingService.getRoleBasedSideMenu(roleId);
                return ResponseEntity.ok(ApiResponse.success("Side menu retrieved successfully", sideMenuList));
        }

        @io.swagger.v3.oas.annotations.Operation(summary = "Get active menu tree")
        @GetMapping("/getActiveMenuTree")
        public ResponseEntity<ApiResponse<List<MenuMappingDTO>>> getActiveMenuTree() {
                List<MenuMappingDTO> result = menuMappingService.getActiveMenuTree();
                return ResponseEntity.ok(ApiResponse.success("Menu tree retrieved successfully", result));
        }

        @io.swagger.v3.oas.annotations.Operation(summary = "Assign role menu privileges")
        @PostMapping("/assign")
        public ResponseEntity<ApiResponse<Void>> assignRoleMenuPrivileges(
                        @RequestBody RoleMenuMappingRequest request) {
                menuMappingService.assignRoleMenuPrivileges(request);
                return ResponseEntity.ok(ApiResponse.success("Role menu privileges assigned successfully!", null));
        }

}
