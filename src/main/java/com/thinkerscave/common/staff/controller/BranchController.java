package com.thinkerscave.common.staff.controller;

import com.thinkerscave.common.staff.service.BranchService;
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
@RequestMapping("/api/branch")
@Tag(name = "Branch Management", description = "Operations related to branch management")
public class BranchController {

    @Autowired
    private BranchService branchService;

    @Operation(summary = "Get All Branches", description = "Retrieve all active branches.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Branches retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Failed to retrieve branches")
    })
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
