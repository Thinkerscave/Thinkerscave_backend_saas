package com.thinkerscave.finance.entity;

import com.thinkerscave.finance.enums.FeePaymentStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fee_payment", indexes = {
        @Index(name = "idx_fee_payment_student_year", columnList = "student_id, academic_year_id"),
        @Index(name = "idx_fee_payment_date", columnList = "paid_on")
})
public class FeePayment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_payment_id")
    @EqualsAndHashCode.Include
    private Long feePaymentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;

    @Column(name = "payment_method_name", nullable = false, length = 80)
    private String paymentMethodName;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_on", nullable = false)
    private LocalDateTime paidOn;

    @Column(name = "reference_number", length = 80)
    private String referenceNumber;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeePaymentStatus status = FeePaymentStatus.SUCCESS;

    @Column(name = "idempotency_key", length = 80, unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "collected_by_user_id")
    private Long collectedByUserId;
}
