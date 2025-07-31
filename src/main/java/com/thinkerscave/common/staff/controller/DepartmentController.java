package com.thinkerscave.common.staff.controller;

import com.thinkerscave.common.staff.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

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
