package com.thinkerscave.common.menum.controller;

import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.dto.MenuDTO;
import com.thinkerscave.common.menum.service.MenuService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/menu")
@Tag(name = "Menu Management", description = "APIs for managing Menus")
public class MenuController {
	
	@Autowired
	private MenuService menuService;

	// ✅ Insert Menu Data
	@PostMapping("/saveMenu")
	public ResponseEntity<Menu> createMenuData(@RequestBody MenuDTO menu) {
		Menu createdMenu = menuService.saveOrUpdateMenu(menu);  // code = null for insert
		return ResponseEntity.status(HttpStatus.CREATED).body(createdMenu);
	}

	// ✅ Display All Menu Data
	@GetMapping("/getAllMenus")
	public ResponseEntity<List<Menu>> displayMenuData() {
		List<Menu> list = menuService.displayMenudata();
		return list.isEmpty()
				? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
				: ResponseEntity.ok(list);
	}
	
	// ✅ Get All Active Menus
	@GetMapping("/activeMenus")
	public ResponseEntity<List<Map<String, Object>>> getAllActiveMenus() {
		List<Map<String, Object>> activeMenus = menuService.getAllActiveMenus();
	    return activeMenus.isEmpty()
	            ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
	            : ResponseEntity.ok(activeMenus);
	}


	// ✅ Get Menu by Code
	@GetMapping("/{code}")
	public ResponseEntity<?> getMenuByCode(@PathVariable String code) {
		Optional<Menu> menu = menuService.displaySingleMenudata(code);
		return menu.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
	}

	// ✅ Toggle Active/Inactive status
	@PutMapping("/updateStatus/{code}")
	public ResponseEntity<Map<String, String>> toggleMenuStatus(
	        @PathVariable String code,
	        @RequestParam boolean status) {
	    String result = menuService.toggleMenuStatus(code, status);
	    return ResponseEntity.ok(Collections.singletonMap("message", result));
	}
}
