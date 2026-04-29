package com.thinkerscave.common.menum.controller;

import com.thinkerscave.common.menum.dto.PrivilegeDTO;
import com.thinkerscave.common.menum.dto.SubMenuRequestDTO;
import com.thinkerscave.common.menum.dto.SubMenuResponseDTO;
import com.thinkerscave.common.menum.service.SubMenuService;
import com.thinkerscave.common.commonModel.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/sub-menus")
@Tag(name = "Submenu Management", description = "APIs for managing Submenus")
@RequiredArgsConstructor
@Slf4j
public class SubMenuController {

        private final SubMenuService subMenuService;

        /**
         * Create or update a submenu.
         *
         * @param subMenuDTO DTO with submenu details
         * @return Created submenu response
         */
        @io.swagger.v3.oas.annotations.Operation(summary = "Create or update submenu")
        @PostMapping
        public ResponseEntity<ApiResponse<SubMenuResponseDTO>> saveSubMenu(@Valid @RequestBody SubMenuRequestDTO subMenuDTO) {
                log.info("API Request - Create/Update SubMenu: {}", subMenuDTO.getSubMenuName());
                SubMenuResponseDTO created = subMenuService.saveOrUpdateSubMenu(subMenuDTO);
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Submenu saved successfully", created));
        }

        /**
         * Get submenu by unique code.
         *
         * @param code submenu code
         * @return Submenu details if found
         */
        @io.swagger.v3.oas.annotations.Operation(summary = "Get submenu by code")
        @GetMapping("/{code}")
        public ResponseEntity<ApiResponse<SubMenuResponseDTO>> getSubMenuByCode(@PathVariable String code) {
                log.info("API Request - Get SubMenu By Code: {}", code);
                Optional<SubMenuResponseDTO> subMenu = subMenuService.getSubMenuByCode(code);
                return subMenu
                                .map(s -> ResponseEntity.ok(ApiResponse.success("Submenu found", s)))
                                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(ApiResponse.error("Submenu not found with code: " + code)));
        }

        /**
         * Get all submenus.
         *
         * @return List of submenus
         */
        @io.swagger.v3.oas.annotations.Operation(summary = "Get all submenus")
        @GetMapping
        public ResponseEntity<ApiResponse<List<SubMenuResponseDTO>>> getAllSubMenus() {
                log.info("API Request - Get All SubMenus");
                List<SubMenuResponseDTO> list = subMenuService.getAllSubMenus();
                return ResponseEntity.ok(ApiResponse.success("Submenus retrieved successfully", list));
        }

        /**
         * Get all active submenus.
         *
         * @return List of active submenus
         */
        @io.swagger.v3.oas.annotations.Operation(summary = "Get active submenus")
        @GetMapping("/activeSubMenus")
        public ResponseEntity<ApiResponse<List<SubMenuResponseDTO>>> getActiveSubMenus() {
                log.info("API Request - Get Active SubMenus");
                List<SubMenuResponseDTO> list = subMenuService.getAllActiveSubMenus();
                return ResponseEntity.ok(ApiResponse.success("Active submenus retrieved successfully", list));
        }

        @io.swagger.v3.oas.annotations.Operation(summary = "Toggle submenu status")
        @PutMapping("/updateStatus/{code}")
        public ResponseEntity<ApiResponse<Void>> toggleMenuStatus(
                        @PathVariable String code,
                        @RequestParam boolean status) {
                log.info("API Request - Toggle SubMenu Status: {} to {}", code, status);
                String result = subMenuService.updateSubMenuStatus(code, status);
                return ResponseEntity.ok(ApiResponse.success(result, null));
        }

        @io.swagger.v3.oas.annotations.Operation(summary = "Get all privileges")
        @GetMapping("/getPrivileges")
        public ResponseEntity<ApiResponse<List<PrivilegeDTO>>> getAllPrivileges() {
                log.info("API Request - Get All Privileges");
                // Need to refactor service to return DTO
                return ResponseEntity.ok(ApiResponse.success("Privileges retrieved successfully", subMenuService.getAllPrivileges().stream()
                        .map(p -> new PrivilegeDTO(p.getPrivilegeId(), p.getPrivilegeName()))
                        .toList()));
        }

}
