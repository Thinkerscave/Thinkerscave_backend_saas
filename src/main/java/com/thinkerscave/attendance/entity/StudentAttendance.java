package com.thinkerscave.attendance.entity;

import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Daily student attendance record — one row per student per date.
 * Unique on (organizationId, studentId, attendanceDate) to prevent duplicates.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "student_attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_attendance_daily",
                        columnNames = {"organization_id", "student_id", "attendance_date"}
                )
        },
        indexes = {
                @Index(name = "idx_sa_org_date", columnList = "organization_id, attendance_date"),
                @Index(name = "idx_sa_class_section_date", columnList = "class_id, section_id, attendance_date"),
                @Index(name = "idx_sa_student_date", columnList = "student_id, attendance_date"),
                @Index(name = "idx_sa_status", columnList = "status"),
                @Index(name = "idx_sa_academic_year", columnList = "academic_year_id")
        }
)
public class StudentAttendance extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    @EqualsAndHashCode.Include
    private Long attendanceId;

    /** Tenant / branch scoping */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // ─── Student Reference (denormalized for query performance) ────────────

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 200)
    private String studentName;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Column(name = "admission_number", length = 50)
    private String admissionNumber;

    // ─── Class / Section Reference ─────────────────────────────────────────

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
