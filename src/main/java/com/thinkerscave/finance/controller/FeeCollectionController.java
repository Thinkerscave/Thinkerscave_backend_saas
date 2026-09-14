package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.request.CollectFeeRequest;
import com.thinkerscave.finance.dto.response.AllocationResponse;
import com.thinkerscave.finance.dto.response.CollectFeeResponse;
import com.thinkerscave.finance.dto.response.FeePaymentResponse;
import com.thinkerscave.finance.dto.response.PaymentPreviewResponse;
import com.thinkerscave.finance.service.FeeCollectionService;
import com.thinkerscave.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fees/payments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FeeCollectionController {

    private final FeeCollectionService feeCollectionService;

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<PaymentPreviewResponse>> preview(@Valid @RequestBody CollectFeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment preview", feeCollectionService.preview(request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollectFeeResponse>> collect(
            @Valid @RequestBody CollectFeeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.status(201).body(ApiResponse.created("Payment collected",
                feeCollectionService.collect(request, idempotencyKey)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeePaymentResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment", feeCollectionService.getPayment(id)));
    }

    @GetMapping("/{id}/allocations")
    public ResponseEntity<ApiResponse<List<AllocationResponse>>> allocations(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Allocations", feeCollectionService.getAllocations(id)));
    }
}
