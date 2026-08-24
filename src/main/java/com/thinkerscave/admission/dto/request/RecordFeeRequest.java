package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.FeePaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecordFeeRequest {

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Receipt number is required")
    private String receiptNumber;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode;

    private LocalDate paidOn;

    private String receivedBy;

    private String remarks;

    private FeePaymentStatus paymentStatus;
}
