package com.thinkerscave.finance.payroll.entity;

import com.thinkerscave.finance.payroll.enums.PaymentType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "employee_salary", indexes = {
        @Index(name = "idx_employee_salary_staff_active", columnList = "staff_id,active"),
        @Index(name = "idx_employee_salary_staff_from", columnList = "staff_id,effective_from")
})
public class EmployeeSalary extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_salary_id")
    @EqualsAndHashCode.Include
    private Long employeeSalaryId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(name = "salary_structure_id")
    private Long salaryStructureId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
