package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.BillingPeriodStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OutstandingItemResponse {
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String className;
    private String sectionName;
    private Long studentBillingPeriodId;
    private String periodKey;
    private String periodLabel;
    private LocalDate dueDate;
    private BigDecimal balanceAmount;
    private BillingPeriodStatus status;
    private boolean canCollectFee;
}
