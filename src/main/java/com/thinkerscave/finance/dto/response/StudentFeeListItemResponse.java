package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.BillingPeriodStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StudentFeeListItemResponse {
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String className;
    private String sectionName;
    private BigDecimal totalFee;
    private BigDecimal paid;
    private BigDecimal outstanding;
    private BigDecimal overdue;
    private BigDecimal advance;
    /** Aggregated student payment posture for the academic year. */
    private BillingPeriodStatus status;
    private boolean canCollectFee;
}
