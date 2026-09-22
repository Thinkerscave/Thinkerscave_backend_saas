package com.thinkerscave.finance.payroll.entity;

import com.thinkerscave.finance.payroll.enums.PayrollRunStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "payroll_run", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_run_year_month", columnNames = {"payroll_year", "payroll_month"})
})
public class PayrollRun extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_run_id")
    @EqualsAndHashCode.Include
    private Long payrollRunId;

    @Column(name = "payroll_year", nullable = false)
    private Integer payrollYear;

    @Column(name = "payroll_month", nullable = false)
    private Integer payrollMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayrollRunStatus status;

    @Column(name = "generated_on")
    private OffsetDateTime generatedOn;

    @Column(name = "approved_on")
    private OffsetDateTime approvedOn;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "returned_on")
    private OffsetDateTime returnedOn;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;
}
