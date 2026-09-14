package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
import com.thinkerscave.finance.service.FeeReceiptService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.util.PageRequestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fees/receipts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FeeReceiptController {

    private final FeeReceiptService feeReceiptService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeeReceiptResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Receipts",
                feeReceiptService.list(q, academicYearId, PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeReceiptResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Receipt", feeReceiptService.get(id)));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<FeeReceiptResponse>> preview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Receipt preview", feeReceiptService.preview(id)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        byte[] bytes = feeReceiptService.pdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
