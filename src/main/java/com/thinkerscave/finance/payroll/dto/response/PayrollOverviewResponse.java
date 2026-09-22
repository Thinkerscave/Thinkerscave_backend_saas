package com.thinkerscave.finance.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollOverviewResponse {
    private Integer year;
    private Integer month;
    private String periodLabel;
    private Long totalStaff;
    private Long generatedCount;
    private Long pendingApprovalCount;
    private Long approvedCount;
    private Long partiallyPaidCount;
    private Long paidCount;
    private Long notGeneratedCount;
    private BigDecimal totalNetAmount;
    private BigDecimal totalPaidAmount;
    private Long runId;
    private String runStatus;
}
