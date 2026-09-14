package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OutstandingSummaryResponse {
    private BigDecimal totalOutstanding;
    private long overdueCount;
    private long dueCount;
    private long partiallyPaidCount;
}
