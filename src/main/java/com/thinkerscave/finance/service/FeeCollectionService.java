package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.request.CollectFeeRequest;
import com.thinkerscave.finance.dto.response.CollectFeeResponse;
import com.thinkerscave.finance.dto.response.FeePaymentResponse;
import com.thinkerscave.finance.dto.response.PaymentPreviewResponse;

import java.util.List;

public interface FeeCollectionService {
    PaymentPreviewResponse preview(CollectFeeRequest request);
    CollectFeeResponse collect(CollectFeeRequest request, String idempotencyKey);
    FeePaymentResponse getPayment(Long paymentId);
    List<com.thinkerscave.finance.dto.response.AllocationResponse> getAllocations(Long paymentId);
}
