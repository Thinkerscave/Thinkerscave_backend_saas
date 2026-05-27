package com.thinkerscave.common.exam.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** One percentage → grade-code bucket inside a {@link GradingScale}. */
@Entity
@Table(name = "grade_boundary",
        indexes = @Index(name = "idx_grade_boundary_scale", columnList = "grading_scale_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeBoundary extends AuditableBaseEntity {

    @Column(name = "grading_scale_id", nullable = false)
    private Long gradingScaleId;

    @Column(name = "grade_code", nullable = false, length = 8)
    private String gradeCode;

    @Column(name = "grade_label", length = 32)
    private String gradeLabel;

    @Column(name = "min_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPercent;

    @Column(name = "max_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercent;

    @Column(name = "grade_point", precision = 4, scale = 2)
    private BigDecimal gradePoint;

    @Column(name = "is_pass", nullable = false)
    private boolean pass;

    @Column(name = "display_order")
    private Integer displayOrder;
}
