package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Refund workflow — refunding overpaid amounts or cancelled-fee amounts back
 * to the payer.
 */
@Entity
@Table(name = "fee_refund",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_refund_number",
                columnNames = {"organization_id", "refund_number"}),
        indexes = {
                @Index(name = "idx_fee_refund_student", columnList = "student_id"),
                @Index(name = "idx_fee_refund_status",  columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeRefund extends OrganizationScopedEntity {

    @Column(name = "refund_number", nullable = false, length = 32)
    private String refundNumber;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "fee_payment_id")
    private Long feePaymentId;

    @Column(name = "fee_invoice_id")
    private Long feeInvoiceId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RefundStatus status;

    @Column(name = "requested_on", nullable = false)
    private LocalDate requestedOn;

    @Column(name = "approved_on")
    private LocalDate approvedOn;

    @Column(name = "processed_on")
    private LocalDate processedOn;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", length = 24)
    private PaymentMethod payoutMethod;

    @Column(name = "payout_reference", length = 64)
    private String payoutReference;
}
