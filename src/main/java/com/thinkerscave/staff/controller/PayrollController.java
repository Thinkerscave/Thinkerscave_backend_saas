package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.staff.dto.request.BulkMarkPaidRequest;
import com.thinkerscave.staff.dto.request.PayrollGenerateRequest;
import com.thinkerscave.staff.dto.response.PayrollDashboardResponse;
import com.thinkerscave.staff.dto.response.PayrollResponse;
import com.thinkerscave.staff.enums.PayrollStatus;
import com.thinkerscave.staff.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@Tag(name = "Payroll Management", description = "APIs for managing staff payroll")
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get payroll dashboard metrics for current month")
    public ResponseEntity<ApiResponse<PayrollDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Payroll dashboard retrieved", payrollService.getDashboard()));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Generate payroll for a given year and month")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generatePayroll(
            @Valid @RequestBody PayrollGenerateRequest request) {
        int count = payrollService.generatePayroll(request);
        return ResponseEntity.status(201).body(
                ApiResponse.created("Payroll generated successfully",
                        Map.of("generatedRecords", count)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get paginated payroll list with filters")
    public ResponseEntity<ApiResponse<Page<PayrollResponse>>> getPayrollList(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) PayrollStatus status,
            @RequestParam(required = false) Long staffId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payroll list retrieved",
                payrollService.getPayrollList(year, month, status, staffId, pageable)));
    }

    @GetMapping("/{payrollId}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get payroll details by ID")
    public ResponseEntity<ApiResponse<PayrollResponse>> getPayrollDetail(@PathVariable Long payrollId) {
        return ResponseEntity.ok(ApiResponse.success("Payroll details retrieved",
                payrollService.getPayrollDetail(payrollId)));
    }

    @PatchMapping("/{payrollId}/mark-paid")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Mark a payroll record as paid")
    public ResponseEntity<ApiResponse<Void>> markPaid(@PathVariable Long payrollId) {
        payrollService.markPaid(payrollId);
        return ResponseEntity.ok(ApiResponse.noContent("Payroll marked as paid"));
    }

    @PatchMapping("/mark-paid")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Bulk mark payroll records as paid")
    public ResponseEntity<ApiResponse<Void>> bulkMarkPaid(@Valid @RequestBody BulkMarkPaidRequest request) {
        payrollService.bulkMarkPaid(request);
        return ResponseEntity.ok(ApiResponse.noContent("Payroll records marked as paid"));
    }

    @GetMapping("/{payrollId}/payslip")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
    @Operation(summary = "Download payslip for a payroll record")
    public ResponseEntity<ApiResponse<PayrollResponse>> downloadPayslip(@PathVariable Long payrollId) {
        // PDF generation is a future enhancement; returning payroll data for now
        return ResponseEntity.ok(ApiResponse.success("Payslip data retrieved",
                payrollService.getPayrollDetail(payrollId)));
    }
}
