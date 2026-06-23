package com.thinkerscave.staff.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.staff.enums.PayrollStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "payroll",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payroll_staff_month",
                        columnNames = {
                                "staff_id",
                                "payroll_year",
                                "payroll_month"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_payroll_staff", columnList = "staff_id"),
                @Index(name = "idx_payroll_status", columnList = "status"),
                @Index(name = "idx_payroll_year_month", columnList = "payroll_year,payroll_month")
        }
)
public class Payroll extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_id")
    @EqualsAndHashCode.Include
    private Long payrollId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @NotNull
    @Column(name = "payroll_year", nullable = false)
    private Integer payrollYear;

    @NotNull
    @Column(name = "payroll_month", nullable = false)
    private Integer payrollMonth;

    @Column(name = "working_days")
    private Integer workingDays;

    @Column(name = "present_days")
    private Integer presentDays;

    @Column(name = "leave_without_pay_days")
    private Integer leaveWithoutPayDays;

    @Column(name = "gross_salary", precision = 12, scale = 2)
    private BigDecimal grossSalary = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 12, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net_salary", precision = 12, scale = 2)
    private BigDecimal netSalary = BigDecimal.ZERO;

    @Column(name = "generated_on")
    private LocalDate generatedOn;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}