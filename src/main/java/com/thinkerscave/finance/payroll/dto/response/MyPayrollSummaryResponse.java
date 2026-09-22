package com.thinkerscave.finance.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MyPayrollSummaryResponse {
    private Long staffId;
    private String staffName;
    private Integer latestYear;
    private Integer latestMonth;
    private BigDecimal latestNetAmount;
    private String latestStatus;
    private List<PayrollEmployeeRowResponse> recent;
}
