package com.thinkerscave.common.role.controller;

import com.thinkerscave.common.role.DTO.RoleDTO;
import com.thinkerscave.common.role.service.RoleService;
import com.thinkerscave.common.role.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/roles")
public class RoleController {

	@Autowired
	private RoleService roleService;

	@Operation(summary = "Welcome endpoint for testing")
	@GetMapping("/")
	public ResponseEntity<String> welcome() {
		return ResponseEntity.ok("Welcome Page");
	}

	@Operation(summary = "Save a new role")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Role saved successfully")
	})
	@PostMapping("/save")
	public ResponseEntity<Role> saveData(@RequestBody RoleDTO roleDto) {
		Role savedRole = roleService.saveData(roleDto);
		return ResponseEntity.ok(savedRole);
	}

	@Operation(summary = "Get all roles")
	@GetMapping("/all")
	public ResponseEntity<List<Role>> allData() {
		List<Role> roles = roleService.allRecords();
		return ResponseEntity.ok(roles);
	}

	@Operation(summary = "Delete a role by ID")
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<String> delete(@PathVariable("id") Long id) {
		roleService.delete(id);
		return ResponseEntity.ok("Deleted successfully");
	}

	@Operation(summary = "Get a role by ID for editing")
	@GetMapping("/edit/{id}")
	public ResponseEntity<Role> editRole(@PathVariable("id") Long id) {
		Role role = roleService.editRoleData(id);
		return ResponseEntity.ok(role);
	}

	@Operation(summary = "Get a role by ID (view only)")
	@GetMapping("/view/{id}")
	public ResponseEntity<Role> viewOne(@PathVariable("id") Long id) {
		Role role = roleService.editRoleData(id); // Same as edit
		return ResponseEntity.ok(role);
	}

	@Operation(summary = "Update an existing role by ID")
	@PutMapping("/update/{id}")
	public ResponseEntity<String> updateRole(@PathVariable Long id, @RequestBody RoleDTO dto) {
		String message = roleService.updateRole(id, dto);
		return ResponseEntity.ok(message);
	}
}
