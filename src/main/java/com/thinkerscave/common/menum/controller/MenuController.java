package com.thinkerscave.common.menum.controller;


import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.dto.MenuDTO;
import com.thinkerscave.common.menum.repository.MenuRepo;
import com.thinkerscave.common.menum.service.MenuService;
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
public class MenuController {

	@Autowired
	private MenuRepo menuRepo;

	@Autowired
	private MenuService menuService;

	// ✅ Insert Menu Data
	@PostMapping
	public ResponseEntity<Menu> createMenuData(@RequestBody MenuDTO menu) {
		Menu insertMenu = menuService.InsertMenu(menu);
		return ResponseEntity.status(HttpStatus.CREATED).body(insertMenu);
	}

	// ✅ Update Menu Data
	@PutMapping("/{id}")
	public ResponseEntity<Menu> updateMenuData(@PathVariable Long id, @RequestBody MenuDTO menu) {
		Menu updatedMenu = menuService.updateMenudata(id, menu);
		return ResponseEntity.ok(updatedMenu);
	}

	// ✅ Display All Menu Data
	@GetMapping
	public ResponseEntity<List<Menu>> displayMenuData() {
		List<Menu> list = menuService.displayMenudata();
		return list.isEmpty()
				? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
				: ResponseEntity.ok(list);
	}

	// ✅ Get Menu by ID
	@GetMapping("/{id}")
	public ResponseEntity<?> getMenuById(@PathVariable Long id) {
		Optional<Menu> menu = menuService.displaySingleMenudata(id);
		if (menu.isPresent()) {
			return ResponseEntity.ok(menu.get());
		}else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

	}

	// ✅ Soft Delete Menu
	@PutMapping("/delete/{id}")
	public ResponseEntity<Map<String, String>> softDeleteMenu(@PathVariable Long id) {
		String result = menuService.softDeleteMenu(id);
		return ResponseEntity.ok(Collections.singletonMap("message", result));
	}

	// ✅ Get All Active Menus
	@GetMapping("/active")
	public ResponseEntity<List<Menu>> getActiveMenus() {
		List<Menu> allActiveMenus = menuRepo.findByIsActiveTrue();
		return allActiveMenus.isEmpty()
				? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
				: ResponseEntity.ok(allActiveMenus);
	}
}
