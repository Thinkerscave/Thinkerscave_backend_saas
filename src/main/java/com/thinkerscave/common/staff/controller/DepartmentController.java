package com.thinkerscave.common.staff.controller;

import com.thinkerscave.common.staff.domain.Department;
import com.thinkerscave.common.staff.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Department", description = "Operations related to department management")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Get All Departments", description = "Retrieve all active departments for the current organization.")
    @GetMapping("/getAllDepartment")
    public ResponseEntity<Map<String, Object>> getAllDepartmentDetails() {
        Map<String, Object> result = departmentService.getAllActiveDepartment();
        return Boolean.TRUE.equals(result.get("isOutcome"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @Operation(summary = "Create or Update Department")
    @PostMapping("/saveOrUpdate")
    public ResponseEntity<Map<String, Object>> saveOrUpdate(@RequestBody Department department) {
        log.info("Saving/updating department: {}", department.getDepartmentName());
        Map<String, Object> result = departmentService.saveOrUpdate(department);
        return Boolean.TRUE.equals(result.get("isOutcome"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @Operation(summary = "Toggle Department Active Status")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Long id) {
        Map<String, Object> result = departmentService.toggleActive(id);
        return Boolean.TRUE.equals(result.get("isOutcome"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
}
