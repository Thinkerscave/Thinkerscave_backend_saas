package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Adjustment applied to a {@link FeeInvoice} — discount, scholarship,
 * waiver, penalty, write-off, etc.
 */
@Entity
@Table(name = "fee_adjustment",
        indexes = {
                @Index(name = "idx_fee_adj_invoice", columnList = "fee_invoice_id"),
                @Index(name = "idx_fee_adj_student", columnList = "student_id"),
                @Index(name = "idx_fee_adj_type",    columnList = "adjustment_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeAdjustment extends OrganizationScopedEntity {

    @Column(name = "fee_invoice_id")
    private Long feeInvoiceId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 24)
    private AdjustmentType adjustmentType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "reason", length = 500)
    private String reason;
}
