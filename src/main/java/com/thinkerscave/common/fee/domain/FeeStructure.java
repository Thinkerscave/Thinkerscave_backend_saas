package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.common.enums.GenericStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Annual fee structure applied to a class (and optionally a section) for a
 * given academic year. Items / line amounts live in {@link FeeStructureItem}.
 */
@Entity
@Table(name = "fee_structure",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_struct_org_year_class_section",
                columnNames = {"organization_id", "academic_year_id", "class_id", "section_id"}),
        indexes = @Index(name = "idx_fee_struct_year_class",
                columnList = "academic_year_id,class_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeStructure extends OrganizationScopedEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "fee_policy_id")
    private Long feePolicyId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GenericStatus status;

    @Column(name = "notes", length = 500)
    private String notes;
}
