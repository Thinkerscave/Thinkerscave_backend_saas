package com.thinkerscave.common.enrollment.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Per-year enrollment record — extracted from the monolithic {@code Student}
 * entity so that one student can have multiple yearly enrollments (each with
 * its own class/section/roll/result trail).
 *
 * <p>{@code studentId} → identity record in {@code Student}. The student
 * entity itself only carries identity fields (name, DOB, contact,
 * parent links). All academic-year-bound facts live here.
 */
@Entity
@Table(name = "academic_enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_enrollment_student_year",
                        columnNames = {"student_id", "academic_year_id"})
        },
        indexes = {
                @Index(name = "idx_enrollment_year_class", columnList = "academic_year_id,class_id"),
                @Index(name = "idx_enrollment_student", columnList = "student_id"),
                @Index(name = "idx_enrollment_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicEnrollment extends OrganizationScopedEntity {

    @Column(name = "enrollment_number", nullable = false, length = 32)
    private String enrollmentNumber;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "roll_number", length = 16)
    private String rollNumber;

    @Column(name = "house", length = 32)
    private String house;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EnrollmentStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
