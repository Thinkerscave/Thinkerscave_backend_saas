package com.thinkerscave.common.exam.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A scheduled examination — e.g. "Mid-Term 2026-27 for Class 10". Holds
 * year/class/section scope and overall window. Per-subject schedule lives in
 * {@link ExamSchedule}; the subjects in scope live in {@link ExamSubject}.
 */
@Entity
@Table(name = "exam",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_org_code",
                columnNames = {"organization_id", "code"}),
        indexes = {
                @Index(name = "idx_exam_year_class", columnList = "academic_year_id,class_id"),
                @Index(name = "idx_exam_status",     columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam extends OrganizationScopedEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "exam_type_id", nullable = false)
    private Long examTypeId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "grading_scale_id")
    private Long gradingScaleId;

    @Column(name = "report_card_template_id")
    private Long reportCardTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ExamStatus status;

    @Column(name = "instructions", length = 1000)
    private String instructions;
}
