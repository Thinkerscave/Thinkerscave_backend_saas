package com.thinkerscave.common.menum.controller;

import com.thinkerscave.common.menum.dto.SubMenuRequestDTO;
import com.thinkerscave.common.menum.dto.SubMenuResponseDTO;
import com.thinkerscave.common.menum.service.SubMenuService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/sub-menus")
@Tag(name = "Submenu Management", description = "APIs for managing Submenus")
@RequiredArgsConstructor
public class SubMenuController {

    private final SubMenuService subMenuService;

    /**
     * Create or update a submenu.
     *
     * @param subMenuDTO DTO with submenu details
     * @return Created submenu response
     */
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
    @GetMapping("/activeSubMenus")
    public ResponseEntity<List<SubMenuResponseDTO>> getActiveSubMenus() {
        List<SubMenuResponseDTO> list = subMenuService.getAllActiveSubMenus();
        return list.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(list);
    }
    
    @PutMapping("/updateStatus/{code}")
	public ResponseEntity<Map<String, String>> toggleMenuStatus(
	        @PathVariable String code,
	        @RequestParam boolean status) {
	    String result = subMenuService.updateSubMenuStatus(code, status);
	    return ResponseEntity.ok(Collections.singletonMap("message", result));
	}

}
