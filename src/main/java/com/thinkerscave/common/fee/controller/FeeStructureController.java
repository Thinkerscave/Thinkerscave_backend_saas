package com.thinkerscave.common.fee.controller;

import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.fee.dto.FeeStructureDTO;
import com.thinkerscave.common.fee.service.FeeStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fee/structures")
@Tag(name = "Fee Structures", description = "Annual fee structures per class & academic year")
@RequiredArgsConstructor
@Slf4j
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    @Operation(summary = "List fee structures for an academic year")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_STRUCTURE_VIEW')")
    public ResponseEntity<ApiResponse<List<FeeStructureDTO>>> list(@RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(feeStructureService.listByYear(academicYearId)));
    }

    @Operation(summary = "Get a fee structure")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_STRUCTURE_VIEW')")
    public ResponseEntity<ApiResponse<FeeStructureDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(feeStructureService.get(id)));
    }

    @Operation(summary = "Compute the annual total for a structure")
    @GetMapping("/{id}/annual-total")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_STRUCTURE_VIEW')")
    public ResponseEntity<ApiResponse<BigDecimal>> total(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(feeStructureService.computeAnnualTotal(id)));
    }

    @Operation(summary = "Create or update a fee structure (with items)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_STRUCTURE_EDIT')")
    public ResponseEntity<ApiResponse<FeeStructureDTO>> save(@Valid @RequestBody FeeStructureDTO dto) {
        FeeStructureDTO saved = feeStructureService.save(dto);
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Fee structure created" : "Fee structure updated", saved));
    }

    @Operation(summary = "Delete a fee structure")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_STRUCTURE_EDIT')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feeStructureService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Fee structure deleted", null));
    }
}
