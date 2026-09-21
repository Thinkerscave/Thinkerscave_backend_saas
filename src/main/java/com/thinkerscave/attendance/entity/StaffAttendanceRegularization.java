package com.thinkerscave.attendance.entity;

import com.thinkerscave.attendance.enums.RegularizationRequestStatus;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Staff attendance correction request. Does not mutate attendance until APPROVED.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "staff_attendance_regularization",
        indexes = {
                @Index(name = "idx_sar_org_staff", columnList = "organization_id, staff_id"),
                @Index(name = "idx_sar_org_status", columnList = "organization_id, status"),
                @Index(name = "idx_sar_org_date", columnList = "organization_id, attendance_date")
        }
)
public class StaffAttendanceRegularization extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    @EqualsAndHashCode.Include
    private Long requestId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "staff_name", nullable = false, length = 200)
    private String staffName;

    @Column(name = "department", length = 100)
    private String department;

    /** Optional link to existing attendance row (may be null when absent). */
    @Column(name = "attendance_id")
    private Long attendanceId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_status", nullable = false, length = 20)
    private StaffAttendanceStatus requestedStatus;

    @Column(name = "requested_sign_in_time")
    private LocalDateTime requestedSignInTime;

    @Column(name = "requested_sign_out_time")
    private LocalDateTime requestedSignOutTime;

    @Column(name = "requested_working_minutes")
    private Integer requestedWorkingMinutes;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegularizationRequestStatus status = RegularizationRequestStatus.PENDING;

    @Column(name = "requested_by", nullable = false, length = 150)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "decision_by", length = 150)
    private String decisionBy;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @Column(name = "decision_comment", length = 1000)
    private String decisionComment;

    // Snapshot of attendance before decision (audit)
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private StaffAttendanceStatus previousStatus;

    @Column(name = "previous_sign_in_time")
    private LocalDateTime previousSignInTime;

    @Column(name = "previous_sign_out_time")
    private LocalDateTime previousSignOutTime;

    @Column(name = "previous_working_minutes")
    private Integer previousWorkingMinutes;
}
