package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StudentFeeKpiResponse {
    private BigDecimal totalFee;
    private BigDecimal paid;
    private BigDecimal outstanding;
    private BigDecimal overdue;
    private BigDecimal advance;
}
