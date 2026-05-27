package com.thinkerscave.common.fee.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import com.thinkerscave.common.fee.domain.InvoiceStatus;
import com.thinkerscave.common.fee.dto.FeeInvoiceDTO;
import com.thinkerscave.common.fee.service.FeeInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/fee/invoices")
@Tag(name = "Fee Invoices", description = "Generate & query student fee invoices")
@RequiredArgsConstructor
@Slf4j
public class FeeInvoiceController {

    private final FeeInvoiceService feeInvoiceService;

    @Operation(summary = "List invoices for a student")
    @GetMapping("/by-student/{studentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_INVOICE_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<FeeInvoiceDTO>>> byStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                feeInvoiceService.listByStudent(studentId, PageRequestUtil.of(page, size, sort)))));
    }

    @Operation(summary = "List invoices by status")
    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_INVOICE_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<FeeInvoiceDTO>>> byStatus(
            @PathVariable InvoiceStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                feeInvoiceService.listByStatus(status, PageRequestUtil.of(page, size, sort)))));
    }

    @Operation(summary = "Get an invoice")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_INVOICE_VIEW')")
    public ResponseEntity<ApiResponse<FeeInvoiceDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(feeInvoiceService.get(id)));
    }

    @Operation(summary = "Lookup invoice by its public invoice number")
    @GetMapping("/by-number/{invoiceNumber}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_INVOICE_VIEW')")
    public ResponseEntity<ApiResponse<FeeInvoiceDTO>> byNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(ApiResponse.success(feeInvoiceService.getByNumber(invoiceNumber)));
    }

    @Operation(summary = "Generate an invoice for a fee contract for a billing period")
    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_INVOICE_GENERATE')")
    public ResponseEntity<ApiResponse<FeeInvoiceDTO>> generate(
            @RequestParam Long contractId,
            @RequestParam String periodLabel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        FeeInvoiceDTO invoice = feeInvoiceService.generateForContract(contractId, periodLabel, issueDate, dueDate);
        return ResponseEntity.ok(ApiResponse.created("Invoice generated", invoice));
    }

    @Operation(summary = "Cancel an invoice")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_INVOICE_CANCEL')")
    public ResponseEntity<ApiResponse<FeeInvoiceDTO>> cancel(@PathVariable Long id,
                                                             @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success("Invoice cancelled", feeInvoiceService.cancel(id, reason)));
    }
}
