package com.thinkerscave.finance.dto.response;

import com.thinkerscave.finance.enums.FeeReceiptStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FeeReceiptResponse {
    private Long feeReceiptId;
    private Long feePaymentId;
    private String receiptNumber;
    private LocalDateTime issuedOn;
    private FeeReceiptStatus status;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String className;
    private String sectionName;
    private Long academicYearId;
    private String academicYearName;
    private BigDecimal amount;
    private String paymentMethodName;
    private String referenceNumber;
    private String remarks;
    private String schoolName;
    private String schoolLogoUrl;
    private String schoolAddress;
    private String schoolContact;
    private String currencyCode;
    private List<FeeReceiptLineResponse> lines;
}
