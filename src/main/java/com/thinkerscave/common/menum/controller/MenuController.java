package com.thinkerscave.common.menum.controller;

import com.thinkerscave.common.menum.dto.MenuDTO;
import com.thinkerscave.common.menum.service.MenuService;
import com.thinkerscave.common.commonModel.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/menu")
@Tag(name = "Menu Management", description = "APIs for managing Menus")
@RequiredArgsConstructor
public class MenuController {

	private final MenuService menuService;

	// ✅ Create or Update Menu
	@io.swagger.v3.oas.annotations.Operation(summary = "Create or update menu")
	@PostMapping("/saveMenu")
	public ResponseEntity<ApiResponse<MenuDTO>> createMenuData(@Valid @RequestBody MenuDTO menu) {
		log.info("API Request - Create/Update Menu: {}", menu.getName());
		MenuDTO createdMenu = menuService.saveOrUpdateMenu(menu);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Menu saved successfully", createdMenu));
	}

	// ✅ Display All Menu Data
	@GetMapping("/getAllMenus")
	@io.swagger.v3.oas.annotations.Operation(summary = "Get all menus")
	public ResponseEntity<ApiResponse<List<MenuDTO>>> displayMenuData() {
		log.info("API Request - Get All Menus");
		List<MenuDTO> list = menuService.displayMenudata();
		return ResponseEntity.ok(ApiResponse.success("Menus retrieved successfully", list));
	}

	// ✅ Get All Active Menus
	@GetMapping("/activeMenus")
	@io.swagger.v3.oas.annotations.Operation(summary = "Get active menus lookup")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllActiveMenus() {
		log.info("API Request - Get All Active Menus");
		List<Map<String, Object>> activeMenus = menuService.getAllActiveMenus();
		return ResponseEntity.ok(ApiResponse.success("Active menus retrieved successfully", activeMenus));
	}

	// ✅ Get Menu by Code
	@GetMapping("/{code}")
	@io.swagger.v3.oas.annotations.Operation(summary = "Get menu by code")
	public ResponseEntity<ApiResponse<MenuDTO>> getMenuByCode(@PathVariable String code) {
		log.info("API Request - Get Menu By Code: {}", code);
		Optional<MenuDTO> menu = menuService.displaySingleMenudata(code);
		return menu.map(m -> ResponseEntity.ok(ApiResponse.success("Menu found", m)))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Menu not found with code: " + code)));
	}

	// ✅ Toggle Active/Inactive status
	@PutMapping("/updateStatus/{code}")
	@io.swagger.v3.oas.annotations.Operation(summary = "Toggle menu status")
	public ResponseEntity<ApiResponse<Void>> toggleMenuStatus(
			@PathVariable String code,
			@RequestParam boolean status) {
		log.info("API Request - Toggle Menu Status: {} to {}", code, status);
		String result = menuService.toggleMenuStatus(code, status);
		return ResponseEntity.ok(ApiResponse.success(result, null));
	}
}
