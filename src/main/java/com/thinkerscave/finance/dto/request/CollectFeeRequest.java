package com.thinkerscave.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CollectFeeRequest {
    @NotNull
    private Long studentId;
    @NotNull
    private Long academicYearId;
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    @NotNull
    private Long paymentMethodId;
    @NotNull
    private LocalDateTime paidOn;
    @Size(max = 80)
    private String referenceNumber;
    @Size(max = 500)
    private String remarks;
}
