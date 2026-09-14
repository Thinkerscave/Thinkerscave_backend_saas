package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CollectFeeResponse {
    private Long feePaymentId;
    private Long feeReceiptId;
    private String receiptNumber;
    private BigDecimal amount;
    private BigDecimal allocatedAmount;
    private BigDecimal advanceRemaining;
    private List<AllocationResponse> allocations;
}
