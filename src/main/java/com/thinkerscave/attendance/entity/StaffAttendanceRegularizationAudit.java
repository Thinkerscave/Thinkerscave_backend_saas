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
 * Immutable audit trail entry for a regularization approve/reject decision.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "staff_attendance_regularization_audit",
        indexes = {
                @Index(name = "idx_sara_request", columnList = "request_id"),
                @Index(name = "idx_sara_org", columnList = "organization_id")
        }
)
public class StaffAttendanceRegularizationAudit extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    @EqualsAndHashCode.Include
    private Long auditId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "staff_name", length = 200)
    private String staffName;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private StaffAttendanceStatus previousStatus;

    @Column(name = "previous_sign_in_time")
    private LocalDateTime previousSignInTime;

    @Column(name = "previous_sign_out_time")
    private LocalDateTime previousSignOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_status", length = 20)
    private StaffAttendanceStatus requestedStatus;

    @Column(name = "requested_sign_in_time")
    private LocalDateTime requestedSignInTime;

    @Column(name = "requested_sign_out_time")
    private LocalDateTime requestedSignOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private RegularizationRequestStatus action;

    @Column(name = "comment", nullable = false, length = 1000)
    private String comment;

    @Column(name = "action_by", nullable = false, length = 150)
    private String actionBy;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;
}
