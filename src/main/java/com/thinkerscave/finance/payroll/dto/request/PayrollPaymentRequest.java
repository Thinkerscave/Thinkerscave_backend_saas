package com.thinkerscave.finance.payroll.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PayrollPaymentRequest {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotNull
    private LocalDate paidOn;
    private Long paymentMethodId;
    private String paymentMethodName;
    private String referenceNumber;
    private String remarks;
}
