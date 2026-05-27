package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Append-only ledger entry for fee transactions — one row per debit/credit.
 * Powers the student fee statement and reconciliation reports.
 */
@Entity
@Table(name = "fee_ledger_entry",
        indexes = {
                @Index(name = "idx_fee_ledger_student", columnList = "student_id,entry_date"),
                @Index(name = "idx_fee_ledger_invoice", columnList = "fee_invoice_id"),
                @Index(name = "idx_fee_ledger_payment", columnList = "fee_payment_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeLedgerEntry extends AuditableBaseEntity {

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    /** {@code INVOICE}, {@code PAYMENT}, {@code ADJUSTMENT}, {@code REFUND}, {@code LATE_FEE}. */
    @Column(name = "entry_type", nullable = false, length = 24)
    private String entryType;

    @Column(name = "debit_amount", precision = 14, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "credit_amount", precision = 14, scale = 2)
    private BigDecimal creditAmount;

    @Column(name = "running_balance", precision = 14, scale = 2)
    private BigDecimal runningBalance;

    @Column(name = "fee_invoice_id")
    private Long feeInvoiceId;

    @Column(name = "fee_payment_id")
    private Long feePaymentId;

    @Column(name = "fee_adjustment_id")
    private Long feeAdjustmentId;

    @Column(name = "fee_refund_id")
    private Long feeRefundId;

    @Column(name = "description", length = 500)
    private String description;
}
