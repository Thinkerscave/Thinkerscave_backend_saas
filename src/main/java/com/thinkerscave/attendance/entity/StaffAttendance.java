package com.thinkerscave.attendance.entity;

import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Staff daily attendance record with sign-in/sign-out tracking.
 * Unique on (organizationId, staffId, attendanceDate).
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "staff_attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_staff_attendance_daily",
                        columnNames = {"organization_id", "staff_id", "attendance_date"}
                )
        },
        indexes = {
                @Index(name = "idx_stfa_org_date", columnList = "organization_id, attendance_date"),
                @Index(name = "idx_stfa_staff_date", columnList = "staff_id, attendance_date"),
                @Index(name = "idx_stfa_status", columnList = "status"),
                @Index(name = "idx_stfa_dept", columnList = "department")
        }
)
public class StaffAttendance extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    @EqualsAndHashCode.Include
    private Long attendanceId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // ─── Staff Reference (denormalized) ───────────────────────────────────

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "staff_name", nullable = false, length = 200)
    private String staffName;

    @Column(name = "staff_code", length = 50)
    private String staffCode;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "designation", length = 100)
    private String designation;

    // ─── Attendance ────────────────────────────────────────────────────────

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "sign_in_time")
    private LocalDateTime signInTime;

    @Column(name = "sign_out_time")
    private LocalDateTime signOutTime;

    /** Computed: difference between signIn and signOut in minutes */
    @Column(name = "working_minutes")
    private Integer workingMinutes;

    @Column(name = "shift", length = 50)
    private String shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StaffAttendanceStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "marked_by", length = 150)
    private String markedBy;
}
