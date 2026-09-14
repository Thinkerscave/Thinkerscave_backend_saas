package com.thinkerscave.finance.payroll.dto.response;

import com.thinkerscave.finance.payroll.enums.PayrollRunStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class PayrollRunResponse {
    private Long payrollRunId;
    private Integer payrollYear;
    private Integer payrollMonth;
    private PayrollRunStatus status;
    private OffsetDateTime generatedOn;
    private OffsetDateTime approvedOn;
    private String approvedBy;
    private OffsetDateTime returnedOn;
    private String notes;
    private List<PayrollEmployeeRowResponse> employees;
}
