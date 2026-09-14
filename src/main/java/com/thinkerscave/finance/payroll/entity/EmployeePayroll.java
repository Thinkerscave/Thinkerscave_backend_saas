package com.thinkerscave.finance.payroll.entity;

import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PaymentType;
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
@Table(name = "employee_payroll", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_payroll_staff_period",
                columnNames = {"staff_id", "payroll_year", "payroll_month"})
}, indexes = {
        @Index(name = "idx_employee_payroll_run", columnList = "payroll_run_id"),
        @Index(name = "idx_employee_payroll_status", columnList = "status")
})
public class EmployeePayroll extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_payroll_id")
    @EqualsAndHashCode.Include
    private Long employeePayrollId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "payroll_year", nullable = false)
    private Integer payrollYear;

    @Column(name = "payroll_month", nullable = false)
    private Integer payrollMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(name = "employment_category", length = 30)
    private String employmentCategory;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "working_days")
    private Integer workingDays;

    @Column(name = "present_days")
    private Integer presentDays;

    @Column(name = "paid_leave_days")
    private Integer paidLeaveDays;

    @Column(name = "lop_days")
    private Integer lopDays;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EmployeePayrollStatus status;

    @Column(name = "payslip_document_id")
    private Long payslipDocumentId;

    @Column(name = "payslip_number", length = 60)
    private String payslipNumber;

    @Column(name = "employee_salary_id")
    private Long employeeSalaryId;
}
