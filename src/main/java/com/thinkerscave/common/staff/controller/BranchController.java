package com.thinkerscave.common.staff.controller;

import com.thinkerscave.common.staff.service.BranchService;
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
@RequestMapping("/api/branch")
public class BranchController {

    @Autowired
    private BranchService branchService;

    @GetMapping("/getAllBranch")
    public ResponseEntity<Map<String, Object>> getAllBranchDetails() {
        Map<String, Object> result = branchService.getAllActiveBranch();

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}
