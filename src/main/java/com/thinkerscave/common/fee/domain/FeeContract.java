package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Per-student fee contract — snapshots the {@link FeeStructure} that applies
 * to one enrollment, with any per-student opt-ins (transport, hostel) and
 * pre-negotiated discounts. Invoices are generated against contracts.
 */
@Entity
@Table(name = "fee_contract",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_contract_enrollment",
                columnNames = {"enrollment_id"}),
        indexes = {
                @Index(name = "idx_fee_contract_org",     columnList = "organization_id"),
                @Index(name = "idx_fee_contract_student", columnList = "student_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeContract extends OrganizationScopedEntity {

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "fee_structure_id", nullable = false)
    private Long feeStructureId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "annual_amount", precision = 14, scale = 2)
    private BigDecimal annualAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "scholarship_amount", precision = 12, scale = 2)
    private BigDecimal scholarshipAmount;

    @Column(name = "net_payable", precision = 14, scale = 2)
    private BigDecimal netPayable;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
