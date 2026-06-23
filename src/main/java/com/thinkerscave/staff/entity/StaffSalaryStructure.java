package com.thinkerscave.staff.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.staff.enums.SalaryType;
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
        name = "staff_salary_structure",
        indexes = {
                @Index(name = "idx_salary_staff", columnList = "staff_id"),
                @Index(name = "idx_salary_active", columnList = "active"),
                @Index(name = "idx_salary_effective_from", columnList = "effective_from")
        }
)
public class StaffSalaryStructure extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_structure_id")
    @EqualsAndHashCode.Include
    private Long salaryStructureId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", nullable = false, length = 20)
    private SalaryType salaryType;

    @Column(name = "basic_pay", precision = 12, scale = 2)
    private BigDecimal basicPay = BigDecimal.ZERO;

    @Column(name = "hra", precision = 12, scale = 2)
    private BigDecimal hra = BigDecimal.ZERO;

    @Column(name = "da", precision = 12, scale = 2)
    private BigDecimal da = BigDecimal.ZERO;

    @Column(name = "special_allowance", precision = 12, scale = 2)
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    @Column(name = "transport_allowance", precision = 12, scale = 2)
    private BigDecimal transportAllowance = BigDecimal.ZERO;

    @Column(name = "other_allowance", precision = 12, scale = 2)
    private BigDecimal otherAllowance = BigDecimal.ZERO;

    @Column(name = "gross_salary", precision = 12, scale = 2)
    private BigDecimal grossSalary = BigDecimal.ZERO;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}