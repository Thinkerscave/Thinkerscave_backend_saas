package com.thinkerscave.common.fee.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import com.thinkerscave.common.fee.dto.FeeContractDTO;
import com.thinkerscave.common.fee.service.FeeContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fee/contracts")
@Tag(name = "Fee Contracts", description = "Per-enrollment fee contracts")
@RequiredArgsConstructor
@Slf4j
public class FeeContractController {

    private final FeeContractService feeContractService;

    @Operation(summary = "List contracts for an academic year")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_CONTRACT_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<FeeContractDTO>>> list(
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                feeContractService.listByYear(academicYearId, PageRequestUtil.of(page, size, sort)))));
    }

    @Operation(summary = "List contracts for a student (across years)")
    @GetMapping("/by-student/{studentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_CONTRACT_VIEW')")
    public ResponseEntity<ApiResponse<List<FeeContractDTO>>> byStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(feeContractService.listByStudent(studentId)));
    }

    @Operation(summary = "Get a contract by enrollment id")
    @GetMapping("/by-enrollment/{enrollmentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_CONTRACT_VIEW')")
    public ResponseEntity<ApiResponse<FeeContractDTO>> byEnrollment(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(ApiResponse.success(feeContractService.getByEnrollment(enrollmentId)));
    }

    @Operation(summary = "Create or update a fee contract")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_CONTRACT_EDIT')")
    public ResponseEntity<ApiResponse<FeeContractDTO>> save(@Valid @RequestBody FeeContractDTO dto) {
        FeeContractDTO saved = feeContractService.save(dto);
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Fee contract created" : "Fee contract updated", saved));
    }

    @Operation(summary = "Delete a fee contract")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_CONTRACT_EDIT')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feeContractService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Fee contract deleted", null));
    }
}
