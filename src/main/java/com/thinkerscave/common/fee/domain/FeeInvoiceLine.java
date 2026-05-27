package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Per-head breakdown of a {@link FeeInvoice}. */
@Entity
@Table(name = "fee_invoice_line",
        indexes = {
                @Index(name = "idx_fee_inv_line_invoice", columnList = "fee_invoice_id"),
                @Index(name = "idx_fee_inv_line_head",    columnList = "fee_head_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeInvoiceLine extends AuditableBaseEntity {

    @Column(name = "fee_invoice_id", nullable = false)
    private Long feeInvoiceId;

    @Column(name = "fee_head_id", nullable = false)
    private Long feeHeadId;

    @Column(name = "description", length = 256)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "display_order")
    private Integer displayOrder;
}
