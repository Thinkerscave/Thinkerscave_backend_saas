package com.thinkerscave.finance.payroll.dto.response;

import com.thinkerscave.finance.payroll.enums.CalculationMethod;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class EmployeePayrollDetailResponse {
    private Long employeePayrollId;
    private Long payrollRunId;
    private Long staffId;
    private String staffCode;
    private String staffName;
    private Integer payrollYear;
    private Integer payrollMonth;
    private PaymentType paymentType;
    private String employmentCategory;
    private String designation;
    private Integer workingDays;
    private Integer presentDays;
    private Integer paidLeaveDays;
    private Integer lopDays;
    private BigDecimal grossAmount;
    private BigDecimal totalDeductions;
    private BigDecimal netAmount;
    private BigDecimal paidAmount;
    private EmployeePayrollStatus status;
    private Long payslipDocumentId;
    private String payslipNumber;
    private List<Line> lines;
    private List<Payment> payments;

    @Data
    @Builder
    public static class Line {
        private Long employeePayrollLineId;
        private String componentCode;
        private String componentName;
        private ComponentType componentType;
        private CalculationMethod calculationMethod;
        private BigDecimal rateOrPercent;
        private BigDecimal amount;
        private Integer sortOrder;
    }

    @Data
    @Builder
    public static class Payment {
        private Long payrollPaymentId;
        private BigDecimal amount;
        private LocalDate paidOn;
        private Long paymentMethodId;
        private String paymentMethodName;
        private String referenceNumber;
        private String remarks;
    }
}
