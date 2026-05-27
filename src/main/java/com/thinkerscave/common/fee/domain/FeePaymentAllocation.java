package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Allocates portions of a {@link FeePayment} against one or more
 * {@link FeeInvoice} rows. Many-to-many bridge that supports
 * advance / partial / over-payments.
 */
@Entity
@Table(name = "fee_payment_allocation",
        indexes = {
                @Index(name = "idx_fee_alloc_payment", columnList = "fee_payment_id"),
                @Index(name = "idx_fee_alloc_invoice", columnList = "fee_invoice_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeePaymentAllocation extends AuditableBaseEntity {

    @Column(name = "fee_payment_id", nullable = false)
    private Long feePaymentId;

    @Column(name = "fee_invoice_id", nullable = false)
    private Long feeInvoiceId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "remarks", length = 256)
    private String remarks;
}
