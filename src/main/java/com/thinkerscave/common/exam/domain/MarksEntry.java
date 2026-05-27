package com.thinkerscave.common.exam.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Per-student per-subject marks row. */
@Entity
@Table(name = "marks_entry",
        uniqueConstraints = @UniqueConstraint(name = "uk_marks_exam_subject_student",
                columnNames = {"exam_id", "subject_id", "student_id"}),
        indexes = {
                @Index(name = "idx_marks_exam_student", columnList = "exam_id,student_id"),
                @Index(name = "idx_marks_status",       columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarksEntry extends OrganizationScopedEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "marks_obtained", precision = 7, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "max_marks", precision = 7, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "grade_code", length = 8)
    private String gradeCode;

    @Column(name = "is_absent", nullable = false)
    private boolean absent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private MarksStatus status;

    @Column(name = "entered_by_user_id")
    private Long enteredByUserId;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "remarks", length = 256)
    private String remarks;
}
