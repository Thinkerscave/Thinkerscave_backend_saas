package com.thinkerscave.common.payroll.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "staff_payroll")
@Data
@NoArgsConstructor
@AllArgsConstructor

@Builder
public class StaffPayroll extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Organization this record belongs to — ensures payroll is isolated per branch
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Unique per organization — same staff member can have different payrolls in
     * different branches
     */
    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "staff_name", nullable = false, length = 200)
    private String staffName;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "designation", length = 100)
    private String designation;

    // ─── Earnings ──────────────────────────────────────────────────────
    @Column(name = "basic", precision = 12, scale = 2)
    private BigDecimal basic = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "hra", precision = 12, scale = 2)
    private BigDecimal hra = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "special_allowance", precision = 12, scale = 2)
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "academic_allowance", precision = 12, scale = 2)
    private BigDecimal academicAllowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "medical_allowance", precision = 12, scale = 2)
    private BigDecimal medicalAllowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "travel_allowance", precision = 12, scale = 2)
    private BigDecimal travelAllowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "dearness_allowance", precision = 12, scale = 2)
    private BigDecimal dearnessAllowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "other_allowance", precision = 12, scale = 2)
    private BigDecimal otherAllowance = BigDecimal.ZERO;

    // ─── Deductions ────────────────────────────────────────────────────
    @Builder.Default
    @Column(name = "professional_tax", precision = 12, scale = 2)
    private BigDecimal professionalTax = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "income_tax", precision = 12, scale = 2)
    private BigDecimal incomeTax = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "provident_fund", precision = 12, scale = 2)
    private BigDecimal providentFund = BigDecimal.ZERO;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /** Computed: gross salary (earnings sum) */
    @Transient
    public BigDecimal getGrossSalary() {
        return basic.add(hra).add(specialAllowance).add(academicAllowance)
                .add(medicalAllowance).add(travelAllowance).add(dearnessAllowance)
                .add(otherAllowance);
    }

    /** Computed: total deductions */
    @Transient
    public BigDecimal getTotalDeductions() {
        return professionalTax.add(incomeTax).add(providentFund);
    }

    /** Computed: net salary */
    @Transient
    public BigDecimal getNetSalary() {
        return getGrossSalary().subtract(getTotalDeductions());
    }
}
