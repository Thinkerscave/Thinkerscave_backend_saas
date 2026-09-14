package com.thinkerscave.finance.payroll.dto.response;

import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PayrollPaymentResponse {
    private Long payrollPaymentId;
    private Long employeePayrollId;
    private BigDecimal amount;
    private LocalDate paidOn;
    private Long paymentMethodId;
    private String paymentMethodName;
    private String referenceNumber;
    private String remarks;
    private BigDecimal totalPaid;
    private BigDecimal remaining;
    private EmployeePayrollStatus employeePayrollStatus;
}
