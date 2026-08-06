package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.staff.dto.request.SalaryStructureRequest;
import com.thinkerscave.staff.dto.response.SalaryStructureResponse;
import com.thinkerscave.staff.service.SalaryStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Salary Structure", description = "APIs for managing staff salary structures")
public class SalaryStructureController {

    private final SalaryStructureService salaryStructureService;

    @PostMapping("/api/v1/staff/salary-structures")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create salary structure for a staff member")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSalaryStructure(
            @Valid @RequestBody SalaryStructureRequest request) {
        Long id = salaryStructureService.createSalaryStructure(request);
        SalaryStructureResponse created = salaryStructureService.getCurrentSalaryStructure(request.getStaffId());
        return ResponseEntity.status(201).body(
                ApiResponse.created("Salary structure created",
                        Map.of("salaryStructureId", id, "grossSalary", created.getGrossSalary())));
    }

    @PutMapping("/api/v1/staff/salary-structures/{id}")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update salary structure")
    public ResponseEntity<ApiResponse<Void>> updateSalaryStructure(
            @PathVariable Long id,
            @Valid @RequestBody SalaryStructureRequest request) {
        salaryStructureService.updateSalaryStructure(id, request);
        return ResponseEntity.ok(ApiResponse.noContent("Salary structure updated successfully"));
    }

    @GetMapping("/api/v1/staff/{staffId}/salary-structure")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get current active salary structure for a staff member")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> getCurrentSalaryStructure(
            @PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Salary structure retrieved",
                salaryStructureService.getCurrentSalaryStructure(staffId)));
    }

    @GetMapping("/api/v1/staff/{staffId}/salary-history")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Get salary history for a staff member")
    public ResponseEntity<ApiResponse<List<SalaryStructureResponse>>> getSalaryHistory(
            @PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Salary history retrieved",
                salaryStructureService.getSalaryHistory(staffId)));
    }
}
