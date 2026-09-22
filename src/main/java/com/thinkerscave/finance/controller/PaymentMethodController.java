package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.request.PaymentMethodRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.PaymentMethodResponse;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.service.PaymentMethodService;
import com.thinkerscave.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fees/payment-methods")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;
    private final FinanceAccessGuard accessGuard;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> list(
            @RequestParam(required = false) FeeMasterStatus status) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_SETTINGS);
        return ResponseEntity.ok(ApiResponse.success("Payment methods", paymentMethodService.list(status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> create(@Valid @RequestBody PaymentMethodRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        return ResponseEntity.status(201).body(ApiResponse.created("Payment method created",
                paymentMethodService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> update(
            @PathVariable Long id, @Valid @RequestBody PaymentMethodRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        return ResponseEntity.ok(ApiResponse.success("Payment method updated",
                paymentMethodService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> status(
            @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        return ResponseEntity.ok(ApiResponse.success("Payment method status updated",
                paymentMethodService.updateStatus(id, request)));
    }
}
