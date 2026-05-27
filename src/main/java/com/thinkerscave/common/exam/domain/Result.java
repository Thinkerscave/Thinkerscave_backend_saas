package com.thinkerscave.common.exam.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Aggregated per-student exam result (totals, percentage, grade, rank). */
@Entity
@Table(name = "result",
        uniqueConstraints = @UniqueConstraint(name = "uk_result_exam_student",
                columnNames = {"exam_id", "student_id"}),
        indexes = {
                @Index(name = "idx_result_exam",   columnList = "exam_id"),
                @Index(name = "idx_result_student", columnList = "student_id"),
                @Index(name = "idx_result_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result extends OrganizationScopedEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "total_marks", precision = 9, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "max_marks", precision = 9, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "gpa", precision = 4, scale = 2)
    private BigDecimal gpa;

    @Column(name = "grade_code", length = 8)
    private String gradeCode;

    @Column(name = "class_rank")
    private Integer classRank;

    @Column(name = "section_rank")
    private Integer sectionRank;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ResultStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
