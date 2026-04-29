package com.thinkerscave.common.payroll.controller;

import com.thinkerscave.common.payroll.dto.PayrollDTO;
import com.thinkerscave.common.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payroll")
@Tag(name = "Payroll Management", description = "APIs for managing staff salary and payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @Operation(summary = "Get all payroll records")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<PayrollDTO>> getAllPayroll() {
        return ResponseEntity.ok(payrollService.getAllPayroll());
    }

    @Operation(summary = "Get payroll for a specific staff")
    @GetMapping("/{staffId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PayrollDTO> getByStaffId(@PathVariable Long staffId) {
        return ResponseEntity.ok(payrollService.getByStaffId(staffId));
    }

    @Operation(summary = "Create or update a staff payroll record")
    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PayrollDTO> saveOrUpdate(
            @RequestBody PayrollDTO dto,
            Authentication auth) {
        String updatedBy = auth != null ? auth.getName() : "SYSTEM";
        return ResponseEntity.ok(payrollService.saveOrUpdate(dto, updatedBy));
    }

    @Operation(summary = "Run payroll for current month")
    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Object>> runPayroll(Authentication auth) {
        String runBy = auth != null ? auth.getName() : "SYSTEM";
        return ResponseEntity.ok(payrollService.runPayroll(runBy));
    }
}
