package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.BillingPeriodStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BillingPeriodResponse {
    private Long studentBillingPeriodId;
    private String periodKey;
    private String periodLabel;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private BillingPeriodStatus status;
    private List<BillingPeriodLineResponse> lines;
}
