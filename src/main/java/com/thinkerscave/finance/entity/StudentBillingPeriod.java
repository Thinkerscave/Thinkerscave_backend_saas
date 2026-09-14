package com.thinkerscave.finance.entity;

import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "student_billing_period",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_billing_period",
                columnNames = {"student_id", "academic_year_id", "period_key"}),
        indexes = {
                @Index(name = "idx_sbp_student_year", columnList = "student_id, academic_year_id"),
                @Index(name = "idx_sbp_year_status", columnList = "academic_year_id, status"),
                @Index(name = "idx_sbp_due_date", columnList = "due_date")
        })
public class StudentBillingPeriod extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_billing_period_id")
    @EqualsAndHashCode.Include
    private Long studentBillingPeriodId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "fee_structure_id", nullable = false)
    private Long feeStructureId;

    @Column(name = "period_key", nullable = false, length = 32)
    private String periodKey;

    @Column(name = "period_label", nullable = false, length = 80)
    private String periodLabel;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Controlled cache: updated only inside allocation transactions. */
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** Controlled cache: paid_amount + balance_amount = total_amount. */
    @Column(name = "balance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BillingPeriodStatus status = BillingPeriodStatus.DUE;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "section_name", length = 100)
    private String sectionName;

    @Column(name = "structure_name", length = 150)
    private String structureName;
}
