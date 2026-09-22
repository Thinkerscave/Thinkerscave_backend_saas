package com.thinkerscave.finance.entity;

import com.thinkerscave.finance.enums.FeeFrequency;
import com.thinkerscave.finance.enums.FeeItemType;
import com.thinkerscave.finance.enums.FeeServiceKey;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Historical fee-head snapshot for a billing period. No paid/balance columns. */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "student_billing_period_line", indexes = {
        @Index(name = "idx_sbpl_period", columnList = "student_billing_period_id"),
        @Index(name = "idx_sbpl_head", columnList = "fee_head_id")
})
public class StudentBillingPeriodLine extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_billing_period_line_id")
    @EqualsAndHashCode.Include
    private Long studentBillingPeriodLineId;

    @Column(name = "student_billing_period_id", nullable = false)
    private Long studentBillingPeriodId;

    @Column(name = "fee_head_id", nullable = false)
    private Long feeHeadId;

    @Column(name = "fee_head_name", nullable = false, length = 100)
    private String feeHeadName;

    @Column(name = "fee_head_category", length = 40)
    private String feeHeadCategory;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private FeeFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private FeeItemType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_key", nullable = false, length = 20)
    private FeeServiceKey serviceKey = FeeServiceKey.NONE;
}
