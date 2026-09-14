package com.thinkerscave.finance.payroll.dto.response;

import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollEmployeeRowResponse {
    private Long staffId;
    private String staffCode;
    private String staffName;
    private String employmentCategory;
    private String designation;
    private PaymentType paymentType;
    private Long employeePayrollId;
    private EmployeePayrollStatus status;
    private String displayStatus;
    private BigDecimal grossAmount;
    private BigDecimal totalDeductions;
    private BigDecimal netAmount;
    private BigDecimal paidAmount;
}
