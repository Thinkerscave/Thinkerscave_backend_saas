package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.FeePaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FeePaymentResponse {
    private Long feePaymentId;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private Long academicYearId;
    private Long paymentMethodId;
    private String paymentMethodName;
    private BigDecimal amount;
    private LocalDateTime paidOn;
    private String referenceNumber;
    private String remarks;
    private FeePaymentStatus status;
    private Long receiptId;
    private String receiptNumber;
    private List<AllocationResponse> allocations;
}
