package com.thinkerscave.common.fee.dto;

import com.thinkerscave.common.fee.domain.AdjustmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeAdjustmentDTO {

    private Long id;

    private Long feeInvoiceId;

    @NotNull
    private Long studentId;

    @NotNull
    private AdjustmentType adjustmentType;

    @NotNull @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate effectiveDate;

    private Long approvedByUserId;

    @Size(max = 500)
    private String reason;
}
