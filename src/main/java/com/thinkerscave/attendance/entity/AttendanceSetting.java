package com.thinkerscave.attendance.entity;

import com.thinkerscave.attendance.enums.AttendanceMode;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Organization-level attendance configuration.
 * One record per organization.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "attendance_setting",
        indexes = {
                @Index(name = "idx_att_setting_org", columnList = "organization_id", unique = true)
        }
)
public class AttendanceSetting extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    @EqualsAndHashCode.Include
    private Long settingId;

    @Column(name = "organization_id", nullable = false, unique = true)
    private Long organizationId;

    // ─── General ──────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_mode", nullable = false, length = 20)
    private AttendanceMode attendanceMode = AttendanceMode.DAILY;

    /** Time after which a student is marked Late (e.g. 08:15) */
    @Column(name = "late_after_time")
    private LocalTime lateAfterTime;

    /** Attendance window start — prevents marking attendance outside this window */
    @Column(name = "window_start_time")
    private LocalTime windowStartTime;

    /** Attendance window end */
    @Column(name = "window_end_time")
    private LocalTime windowEndTime;

    /** Allow copying attendance from the previous working day */
    @Column(name = "allow_copy_previous")
    private Boolean allowCopyPrevious = true;

    // ─── Student Rules ────────────────────────────────────────────────────

    /** Minimum attendance % required (default 75) */
    @Column(name = "min_student_attendance_percent")
    private Integer minStudentAttendancePercent = 75;

    /** Alert parents when attendance falls below threshold */
    @Column(name = "student_alert_threshold_percent")
    private Integer studentAlertThresholdPercent = 80;

    @Column(name = "send_sms_on_absent")
    private Boolean sendSmsOnAbsent = false;

    @Column(name = "send_email_on_absent")
    private Boolean sendEmailOnAbsent = false;

    // ─── Staff Rules ──────────────────────────────────────────────────────

    @Column(name = "min_staff_working_hours")
    private Integer minStaffWorkingHours = 8;

    /** Minutes late before marking LATE */
    @Column(name = "staff_late_grace_minutes")
    private Integer staffLateGraceMinutes = 15;

    // ─── Freeze ───────────────────────────────────────────────────────────

    /** Lock attendance editing after N days. 0 = no freeze. */
    @Column(name = "freeze_after_days")
    private Integer freezeAfterDays = 0;

    @Column(name = "active")
    private Boolean active = true;
}
