package com.thinkerscave.common.staff.controller;

import com.thinkerscave.common.staff.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/branch")
@Tag(name = "Branch Management", description = "Operations related to branch management")
@RequiredArgsConstructor
@Slf4j
public class BranchController {

    private final BranchService branchService;

    @Operation(summary = "Get All Branches", description = "Retrieve all active branches.", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Branches retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Failed to retrieve branches")
    })
    @GetMapping("/getAllBranch")
    public ResponseEntity<Map<String, Object>> getAllBranchDetails() {
        log.info("Fetching all branch details");
        Map<String, Object> result = branchService.getAllActiveBranch();

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}
