package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.request.PaymentMethodRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.PaymentMethodResponse;
import com.thinkerscave.finance.enums.FeeMasterStatus;

import java.util.List;

public interface PaymentMethodService {
    List<PaymentMethodResponse> list(FeeMasterStatus status);
    PaymentMethodResponse create(PaymentMethodRequest request);
    PaymentMethodResponse update(Long id, PaymentMethodRequest request);
    PaymentMethodResponse updateStatus(Long id, StatusUpdateRequest request);
}
