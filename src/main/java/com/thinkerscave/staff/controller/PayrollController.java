package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @deprecated Staff-owned payroll APIs are retired. Use Finance Payroll under {@code /api/v1/payroll}.
 */
@Deprecated
@RestController
@RequestMapping("/api/v1/staff/payroll")
@Tag(name = "Payroll Management (Deprecated)", description = "Retired — use Finance Payroll APIs")
@Hidden
public class PayrollController {

    private static final String GONE_MSG =
            "Staff Payroll APIs are retired. Use Finance Payroll at /api/v1/payroll (Finance module).";

    @GetMapping("/dashboard")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> getDashboard() {
        return gone();
    }

    @PostMapping("/generate")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> generatePayroll(@RequestBody(required = false) Object body) {
        return gone();
    }

    @GetMapping("/report")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> monthlyReport() {
        return gone();
    }

    @GetMapping("/report/export")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> exportMonthlyReport() {
        return gone();
    }

    @GetMapping
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> getPayrollList() {
        return gone();
    }

    @GetMapping("/{payrollId}")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> getPayrollDetail(@PathVariable Long payrollId) {
        return gone();
    }

    @PatchMapping("/{payrollId}/mark-paid")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> markPaid(@PathVariable Long payrollId) {
        return gone();
    }

    @PatchMapping("/mark-paid")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> bulkMarkPaid(@RequestBody(required = false) Object body) {
        return gone();
    }

    @GetMapping("/{payrollId}/payslip")
    @Operation(summary = "Deprecated")
    public ResponseEntity<ApiResponse<Void>> downloadPayslip(@PathVariable Long payrollId) {
        return gone();
    }

    private static ResponseEntity<ApiResponse<Void>> gone() {
        return ResponseEntity.status(HttpStatus.GONE).body(ApiResponse.error(GONE_MSG));
    }
}
