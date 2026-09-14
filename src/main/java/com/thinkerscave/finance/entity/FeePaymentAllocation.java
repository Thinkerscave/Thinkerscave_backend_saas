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
@Table(name = "fee_payment_allocation",
        uniqueConstraints = @UniqueConstraint(name = "uk_fpa_payment_period",
                columnNames = {"fee_payment_id", "student_billing_period_id"}))
public class FeePaymentAllocation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_payment_allocation_id")
    @EqualsAndHashCode.Include
    private Long feePaymentAllocationId;

    @Column(name = "fee_payment_id", nullable = false)
    private Long feePaymentId;

    @Column(name = "student_billing_period_id", nullable = false)
    private Long studentBillingPeriodId;

    @Column(name = "allocated_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal allocatedAmount;
}
