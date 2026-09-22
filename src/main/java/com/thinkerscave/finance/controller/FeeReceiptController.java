package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
import com.thinkerscave.finance.enums.FeeReceiptStatus;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.service.FeeReceiptService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.util.PageRequestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/fees/receipts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FeeReceiptController {

    private final FeeReceiptService feeReceiptService;
    private final FinanceAccessGuard accessGuard;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeeReceiptResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) FeeReceiptStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_RECEIPTS);
        String effectiveSort = (sort == null || sort.isBlank()) ? "issuedOn,desc" : sort;
        return ResponseEntity.ok(ApiResponse.success("Receipts",
                feeReceiptService.list(q, academicYearId, studentId, classId, sectionId, paymentMethod, status,
                        fromDate, toDate, PageRequestUtil.of(page, size, effectiveSort))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeReceiptResponse>> get(@PathVariable Long id) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_RECEIPTS);
        return ResponseEntity.ok(ApiResponse.success("Receipt", feeReceiptService.get(id)));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<FeeReceiptResponse>> preview(@PathVariable Long id) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_RECEIPTS);
        return ResponseEntity.ok(ApiResponse.success("Receipt preview", feeReceiptService.preview(id)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_RECEIPTS);
        byte[] bytes = feeReceiptService.pdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
