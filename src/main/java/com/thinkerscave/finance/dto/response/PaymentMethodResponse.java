package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.FeeMasterStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentMethodResponse {
    private Long feePaymentMethodId;
    private String name;
    private String description;
    private FeeMasterStatus status;
    private boolean requiresReference;
    private Integer sortOrder;
}
