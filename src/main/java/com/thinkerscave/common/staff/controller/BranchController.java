package com.thinkerscave.common.staff.controller;

import com.thinkerscave.common.staff.domain.Branch;
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

@RestController
@RequestMapping("/api/v1/branches")
@Tag(name = "Branch Management", description = "Operations related to branch management")
@RequiredArgsConstructor
@Slf4j
public class BranchController {

    private final BranchService branchService;

    @Operation(summary = "Get All Branches", description = "Retrieve all active branches for the current organization.")
    @GetMapping("/getAllBranch")
    public ResponseEntity<Map<String, Object>> getAllBranchDetails() {
        log.info("Fetching all branch details");
        Map<String, Object> result = branchService.getAllActiveBranch();
        return Boolean.TRUE.equals(result.get("isOutcome"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @Operation(summary = "Create or Update Branch")
    @PostMapping("/saveOrUpdate")
    public ResponseEntity<Map<String, Object>> saveOrUpdate(@RequestBody Branch branch) {
        log.info("Saving/updating branch: {}", branch.getBranchName());
        Map<String, Object> result = branchService.saveOrUpdate(branch);
        return Boolean.TRUE.equals(result.get("isOutcome"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @Operation(summary = "Toggle Branch Active Status")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Long id) {
        log.info("Toggling active status for branch id: {}", id);
        Map<String, Object> result = branchService.toggleActive(id);
        return Boolean.TRUE.equals(result.get("isOutcome"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
}
