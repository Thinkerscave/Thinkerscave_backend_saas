package com.thinkerscave.common.fee.dto;

import com.thinkerscave.common.fee.domain.PaymentMethod;
import com.thinkerscave.common.fee.domain.RefundStatus;
import jakarta.validation.constraints.NotBlank;
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
public class FeeRefundDTO {

    private Long id;

    private String refundNumber;

    @NotNull
    private Long studentId;

    private Long feePaymentId;
    private Long feeInvoiceId;

    @NotNull @Positive
    private BigDecimal amount;

    @NotBlank @Size(max = 500)
    private String reason;

    private RefundStatus status;

    private LocalDate requestedOn;
    private LocalDate approvedOn;
    private LocalDate processedOn;
    private Long approvedByUserId;

    private PaymentMethod payoutMethod;
    private String payoutReference;
}
