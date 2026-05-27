package com.thinkerscave.common.exam.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.common.enums.GenericStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Master of exam types — e.g. {@code UNIT_TEST}, {@code MID_TERM},
 * {@code FINAL}, {@code PRACTICAL}. Drives weightage & report-card grouping.
 */
@Entity
@Table(name = "exam_type",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_type_org_code",
                columnNames = {"organization_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamType extends OrganizationScopedEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "weightage_percent")
    private Integer weightagePercent;

    @Column(name = "is_final_term", nullable = false)
    private boolean finalTerm;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GenericStatus status;
}
