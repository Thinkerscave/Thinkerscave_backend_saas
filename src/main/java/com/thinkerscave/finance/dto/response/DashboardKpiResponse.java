package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardKpiResponse {
    private BigDecimal generated;
    private BigDecimal collected;
    private BigDecimal outstanding;
    private BigDecimal overdue;
    private boolean canCollectFee;
}
