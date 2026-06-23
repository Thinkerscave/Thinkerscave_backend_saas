package com.thinkerscave.staff.dto.response;

import com.thinkerscave.staff.enums.PayrollStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PayrollResponse {

    private Long payrollId;
    private Long staffId;
    private String staffName;
    private String staffCode;
    private Integer payrollYear;
    private Integer payrollMonth;
    private Integer workingDays;
    private Integer presentDays;
    private Integer leaveWithoutPayDays;
    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private PayrollStatus status;
    private LocalDate generatedOn;
    private LocalDate paidOn;
    private String remarks;
    private LocalDateTime createdOn;
}
