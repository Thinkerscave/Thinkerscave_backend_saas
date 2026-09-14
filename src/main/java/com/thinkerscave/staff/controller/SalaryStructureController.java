package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @deprecated Staff salary structure APIs are retired. Use Finance Payroll employee salary / structures.
 */
@Deprecated
@RestController
@Tag(name = "Salary Structure (Deprecated)", description = "Retired — use Finance Payroll APIs")
@Hidden
public class SalaryStructureController {

    private static final String GONE_MSG =
            "Staff salary structure APIs are retired. Use Finance Payroll at /api/v1/payroll/structures and /api/v1/payroll/employees/{staffId}/salary.";

    @PostMapping("/api/v1/staff/salary-structures")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> createSalaryStructure(@RequestBody(required = false) Object body) {
        return gone();
    }

    @PutMapping("/api/v1/staff/salary-structures/{id}")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> updateSalaryStructure(
            @PathVariable Long id, @RequestBody(required = false) Object body) {
        return gone();
    }

    @GetMapping("/api/v1/staff/{staffId}/salary-structure")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> getCurrentSalaryStructure(@PathVariable Long staffId) {
        return gone();
    }

    @GetMapping("/api/v1/staff/{staffId}/salary-history")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> getSalaryHistory(@PathVariable Long staffId) {
        return gone();
    }

    private static ResponseEntity<ApiResponse<Void>> gone() {
        return ResponseEntity.status(HttpStatus.GONE).body(ApiResponse.error(GONE_MSG));
    }
}
