package com.thinkerscave.common.fee.dto;

import com.thinkerscave.common.fee.domain.PaymentMethod;
import com.thinkerscave.common.fee.domain.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeePaymentDTO {

    private Long id;

    private String receiptNumber;

    @NotNull
    private Long studentId;

    @NotNull
    private LocalDate paymentDate;

    @NotNull @Positive
    private BigDecimal amount;

    private BigDecimal allocatedAmount;
    private BigDecimal unallocatedAmount;

    @NotNull
    private PaymentMethod paymentMethod;

    @Size(max = 64)
    private String referenceNumber;

    @Size(max = 128)
    private String gatewayTransactionId;

    private Long receivedByUserId;

    private PaymentStatus status;

    @Size(max = 500)
    private String remarks;

    @Valid
    private List<FeePaymentAllocationDTO> allocations;
}
