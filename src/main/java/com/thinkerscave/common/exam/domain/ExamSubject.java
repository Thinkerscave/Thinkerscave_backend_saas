package com.thinkerscave.common.exam.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Subject row within an {@link Exam} — max/passing marks per subject. */
@Entity
@Table(name = "exam_subject",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_subject_exam_subject",
                columnNames = {"exam_id", "subject_id"}),
        indexes = @Index(name = "idx_exam_subject_exam", columnList = "exam_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSubject extends AuditableBaseEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "max_marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "passing_marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal passingMarks;

    @Column(name = "weightage_percent")
    private Integer weightagePercent;

    @Column(name = "is_optional", nullable = false)
    private boolean optional;
}
