package com.thinkerscave.finance.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fee_receipt_line", indexes = {
        @Index(name = "idx_fee_receipt_line_receipt", columnList = "fee_receipt_id")
})
public class FeeReceiptLine extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_receipt_line_id")
    @EqualsAndHashCode.Include
    private Long feeReceiptLineId;

    @Column(name = "fee_receipt_id", nullable = false)
    private Long feeReceiptId;

    @Column(name = "student_billing_period_id")
    private Long studentBillingPeriodId;

    @Column(name = "period_key", nullable = false, length = 32)
    private String periodKey;

    @Column(name = "period_label", nullable = false, length = 80)
    private String periodLabel;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
