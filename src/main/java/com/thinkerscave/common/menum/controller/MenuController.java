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

@CrossOrigin("*")
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
		Menu createdMenu = menuService.saveOrUpdateMenu(null, menu);  // code = null for insert
		return ResponseEntity.status(HttpStatus.CREATED).body(createdMenu);
	}

	// ✅ Update Menu Data by Code
	@PutMapping("/{code}")
	public ResponseEntity<Menu> updateMenuData(@PathVariable String code, @RequestBody MenuDTO menu) {
		Menu updatedMenu = menuService.saveOrUpdateMenu(code, menu);  // code for update
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

	// ✅ Get Menu by Code
	@GetMapping("/{code}")
	public ResponseEntity<?> getMenuByCode(@PathVariable String code) {
		Optional<Menu> menu = menuService.displaySingleMenudata(code);
		return menu.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
	}

	// ✅ Soft Delete Menu by Code
	@PutMapping("/delete/{code}")
	public ResponseEntity<Map<String, String>> softDeleteMenu(@PathVariable String code) {
		String result = menuService.softDeleteMenu(code);
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
