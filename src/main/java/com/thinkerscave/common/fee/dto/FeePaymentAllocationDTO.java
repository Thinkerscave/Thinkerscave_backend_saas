package com.thinkerscave.common.fee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeePaymentAllocationDTO {

    private Long id;

    @NotNull
    private Long feeInvoiceId;

    @NotNull @Positive
    private BigDecimal amount;

    private String remarks;
}
