package com.thinkerscave.common.fee.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeLedgerEntryDTO {

    private Long id;
    private Long studentId;
    private LocalDate entryDate;
    private String entryType;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal runningBalance;
    private Long feeInvoiceId;
    private Long feePaymentId;
    private Long feeAdjustmentId;
    private Long feeRefundId;
    private String description;
}
