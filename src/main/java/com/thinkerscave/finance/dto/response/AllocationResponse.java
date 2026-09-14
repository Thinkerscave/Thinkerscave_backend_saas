package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AllocationResponse {
    private Long feePaymentAllocationId;
    private Long studentBillingPeriodId;
    private String periodKey;
    private String periodLabel;
    private BigDecimal allocatedAmount;
}
