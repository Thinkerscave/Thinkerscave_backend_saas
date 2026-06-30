package com.thinkerscave.attendance.entity;

import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Period-wise student attendance — one row per student per period per date.
 * Unique on (organizationId, studentId, attendanceDate, periodId).
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "student_period_attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_period_attendance",
                        columnNames = {"organization_id", "student_id", "attendance_date", "period_id"}
                )
        },
        indexes = {
                @Index(name = "idx_spa_org_date", columnList = "organization_id, attendance_date"),
                @Index(name = "idx_spa_class_section_date", columnList = "class_id, section_id, attendance_date"),
                @Index(name = "idx_spa_student_date", columnList = "student_id, attendance_date"),
                @Index(name = "idx_spa_period", columnList = "period_id")
        }
)
public class StudentPeriodAttendance extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "period_attendance_id")
    @EqualsAndHashCode.Include
    private Long periodAttendanceId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // ─── Student Reference ─────────────────────────────────────────────────

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 200)
    private String studentName;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    // ─── Academic Reference ────────────────────────────────────────────────

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "section_name", length = 50)
    private String sectionName;

    // ─── Period Reference ──────────────────────────────────────────────────

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "period_number")
    private Integer periodNumber;

    @Column(name = "period_name", length = 100)
    private String periodName;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "subject_name", length = 150)
    private String subjectName;

    // ─── Attendance ────────────────────────────────────────────────────────

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentAttendanceStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "marked_by", length = 150)
    private String markedBy;
}
