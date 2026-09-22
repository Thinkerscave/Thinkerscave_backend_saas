package com.thinkerscave.finance.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Mandatory concurrency lock root for payment collection and fee generation.
 * Must not store any financial balances.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "student_fee_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_fee_account", columnNames = {"student_id", "academic_year_id"}))
public class StudentFeeAccount extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_fee_account_id")
    @EqualsAndHashCode.Include
    private Long studentFeeAccountId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;
}
