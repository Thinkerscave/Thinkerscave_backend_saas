package com.thinkerscave.attendance.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Date ranges where attendance records are frozen (read-only).
 * Admin can create freeze periods to prevent editing historical records.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "attendance_freeze",
        indexes = {
                @Index(name = "idx_att_freeze_org", columnList = "organization_id"),
                @Index(name = "idx_att_freeze_dates", columnList = "organization_id, freeze_from_date, freeze_to_date")
        }
)
public class AttendanceFreeze extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "freeze_id")
    @EqualsAndHashCode.Include
    private Long freezeId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "freeze_from_date", nullable = false)
    private LocalDate freezeFromDate;

    @Column(name = "freeze_to_date", nullable = false)
    private LocalDate freezeToDate;

    @Column(name = "reason", length = 500)
    private String reason;

    /** false = deleted/revoked */
    @Column(name = "active")
    private Boolean active = true;
}
