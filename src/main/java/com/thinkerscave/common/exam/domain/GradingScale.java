package com.thinkerscave.common.exam.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.common.enums.GenericStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Reusable grading scale — e.g. "10-point CGPA", "Letter A+ to F". Concrete
 * percentage→grade buckets live in {@link GradeBoundary}.
 */
@Entity
@Table(name = "grading_scale",
        uniqueConstraints = @UniqueConstraint(name = "uk_grading_scale_org_code",
                columnNames = {"organization_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingScale extends OrganizationScopedEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GenericStatus status;
}
