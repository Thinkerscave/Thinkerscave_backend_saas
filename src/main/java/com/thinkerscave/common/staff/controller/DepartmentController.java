package com.thinkerscave.common.staff.controller;

import com.thinkerscave.common.staff.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/department")
@Tag(name = "Department", description = "Operations related to department management")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Operation(summary = "Get All Departments", description = "Retrieve all active departments.", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Departments retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Failed to retrieve departments")
    })
    @GetMapping("/getAllDepartment")
    public ResponseEntity<Map<String, Object>> getAllDepartmentDetails() {
        Map<String, Object> result = departmentService.getAllActiveDepartment();

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}
