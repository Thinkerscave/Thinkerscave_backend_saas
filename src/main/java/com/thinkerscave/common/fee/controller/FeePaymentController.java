package com.thinkerscave.common.fee.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import com.thinkerscave.common.fee.dto.FeeLedgerEntryDTO;
import com.thinkerscave.common.fee.dto.FeePaymentAllocationDTO;
import com.thinkerscave.common.fee.dto.FeePaymentDTO;
import com.thinkerscave.common.fee.service.FeeLedgerService;
import com.thinkerscave.common.fee.service.FeePaymentService;
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
@RequestMapping("/api/v1/fee/payments")
@Tag(name = "Fee Payments", description = "Record & allocate payments; query ledger")
@RequiredArgsConstructor
@Slf4j
public class FeePaymentController {

    private final FeePaymentService feePaymentService;
    private final FeeLedgerService feeLedgerService;

    @Operation(summary = "List payments for a student")
    @GetMapping("/by-student/{studentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_PAYMENT_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<FeePaymentDTO>>> byStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                feePaymentService.listByStudent(studentId, PageRequestUtil.of(page, size, sort)))));
    }

    @Operation(summary = "Get a payment by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_PAYMENT_VIEW')")
    public ResponseEntity<ApiResponse<FeePaymentDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(feePaymentService.get(id)));
    }

    @Operation(summary = "Record a new payment (with optional allocations)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_PAYMENT_RECORD')")
    public ResponseEntity<ApiResponse<FeePaymentDTO>> record(@Valid @RequestBody FeePaymentDTO dto) {
        FeePaymentDTO saved = feePaymentService.record(dto);
        return ResponseEntity.ok(ApiResponse.created("Payment recorded", saved));
    }

    @Operation(summary = "Add allocations to an existing payment with unallocated balance")
    @PostMapping("/{id}/allocate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_PAYMENT_RECORD')")
    public ResponseEntity<ApiResponse<FeePaymentDTO>> allocate(
            @PathVariable Long id,
            @Valid @RequestBody List<FeePaymentAllocationDTO> allocations) {
        return ResponseEntity.ok(ApiResponse.success("Allocations applied",
                feePaymentService.allocate(id, allocations)));
    }

    @Operation(summary = "Get a student's running fee statement (ledger)")
    @GetMapping("/ledger/{studentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_LEDGER_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<FeeLedgerEntryDTO>>> ledger(
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                feeLedgerService.statementOf(studentId, PageRequestUtil.of(page, size, sort)))));
    }

    @Operation(summary = "Get a student's current fee balance")
    @GetMapping("/balance/{studentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_LEDGER_VIEW')")
    public ResponseEntity<ApiResponse<BigDecimal>> balance(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(feeLedgerService.currentBalance(studentId)));
    }
}
