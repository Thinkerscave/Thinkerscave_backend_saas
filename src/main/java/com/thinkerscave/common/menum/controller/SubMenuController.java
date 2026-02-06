package com.thinkerscave.common.menum.controller;

import com.thinkerscave.common.menum.domain.Privilege;
import com.thinkerscave.common.menum.dto.SubMenuRequestDTO;
import com.thinkerscave.common.menum.dto.SubMenuResponseDTO;
import com.thinkerscave.common.menum.service.SubMenuService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sub-menus")
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
    @io.swagger.v3.oas.annotations.Operation(summary = "Create or update submenu", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @PostMapping
    public ResponseEntity<SubMenuResponseDTO> saveSubMenu(@RequestBody SubMenuRequestDTO subMenuDTO) {
        SubMenuResponseDTO created = subMenuService.saveOrUpdateSubMenu(subMenuDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get submenu by unique code.
     *
     * @param code submenu code
     * @return Submenu details if found
     */
    @io.swagger.v3.oas.annotations.Operation(summary = "Get submenu by code", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping("/{code}")
    public ResponseEntity<?> getSubMenuByCode(@PathVariable String code) {
        Optional<SubMenuResponseDTO> subMenu = subMenuService.getSubMenuByCode(code);
        return subMenu
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Submenu not found with code: " + code));
    }

    /**
     * Get all submenus.
     *
     * @return List of submenus
     */
    @io.swagger.v3.oas.annotations.Operation(summary = "Get all submenus", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping
    public ResponseEntity<List<SubMenuResponseDTO>> getAllSubMenus() {
        List<SubMenuResponseDTO> list = subMenuService.getAllSubMenus();
        return list.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(list);
    }

    /**
     * Get all active submenus.
     *
     * @return List of active submenus
     */
    @io.swagger.v3.oas.annotations.Operation(summary = "Get active submenus", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping("/activeSubMenus")
    public ResponseEntity<List<SubMenuResponseDTO>> getActiveSubMenus() {
        List<SubMenuResponseDTO> list = subMenuService.getAllActiveSubMenus();
        return list.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(list);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Toggle submenu status", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @PutMapping("/updateStatus/{code}")
    public ResponseEntity<Map<String, String>> toggleMenuStatus(
            @PathVariable String code,
            @RequestParam boolean status) {
        String result = subMenuService.updateSubMenuStatus(code, status);
        return ResponseEntity.ok(Collections.singletonMap("message", result));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get all privileges", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping("/getPrivileges")
    public ResponseEntity<List<Privilege>> getAllPrivileges() {
        log.info("API called: Get All Privileges");
        return ResponseEntity.ok(subMenuService.getAllPrivileges());
    }

}
