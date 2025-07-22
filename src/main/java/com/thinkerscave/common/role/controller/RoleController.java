package com.thinkerscave.common.role.controller;

import com.thinkerscave.common.role.dto.RoleDTO;
import com.thinkerscave.common.role.service.RoleService;
import com.thinkerscave.common.role.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

	@Autowired
	private RoleService roleService;

	@Operation(summary = "Welcome endpoint for testing")
	@GetMapping("/")
	public ResponseEntity<String> welcome() {
		return ResponseEntity.ok("Welcome Page");
	}

	@Operation(summary = "Create a new role")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Role created successfully")
	})
	@PostMapping("/save")
	public ResponseEntity<String> saveRole(@RequestBody RoleDTO roleDto) {
		String message = roleService.saveOrUpdateRole(null, roleDto); // Pass null for creation
		return ResponseEntity.ok(message);
	}

	@Operation(summary = "Update an existing role by ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Role updated successfully"),
			@ApiResponse(responseCode = "404", description = "Role not found")
	})

	@PutMapping("/update/{code}")
	public ResponseEntity<String> updateRole(@PathVariable String code, @RequestBody RoleDTO roleDto) {
		String message = roleService.saveOrUpdateRole(code, roleDto);
		return ResponseEntity.ok(message);
	}

	@Operation(summary = "Get all roles")
	@GetMapping("/all")
	public ResponseEntity<List<Role>> allData() {
		List<Role> roles = roleService.allRecords();
		return ResponseEntity.ok(roles);
	}

	@Operation(summary = "Delete a role by ID (soft delete)")
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<String> delete(@PathVariable("id") String id) {
		roleService.delete(id);
		return ResponseEntity.ok("Deleted successfully");
	}

	@Operation(summary = "Get a role by ID (view only)")
	@GetMapping("/view/{id}")
	public ResponseEntity<Role> viewOne(@PathVariable("id") String id) {
		Role role = roleService.editRoleData(id); // Same as view
		return ResponseEntity.ok(role);
	}
}
