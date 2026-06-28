package com.thinkerscave.platform.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "maintenance_schedules",
        indexes = {
                @Index(name = "idx_maintenance_org", columnList = "organization_id"),
                @Index(name = "idx_maintenance_start", columnList = "start_time"),
                @Index(name = "idx_maintenance_end", columnList = "end_time"),
                @Index(name = "idx_maintenance_active", columnList = "active")
        }
)
public class MaintenanceSchedule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Organization.
     * Null means platform-wide maintenance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * Maintenance Title.
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Description.
     */
    @Column(name = "description", length = 2000)
    private String description;

    /**
     * Maintenance Reason.
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Scheduled Start Time.
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * Scheduled End Time.
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * Actual Start Time.
     */
    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    /**
     * Actual End Time.
     */
    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    /**
     * Planned Maintenance.
     */
    @Builder.Default
    @Column(name = "planned")
    private Boolean planned = true;

    /**
     * Notification Sent.
     */
    @Builder.Default
    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    /**
     * Maintenance Completed.
     */
    @Builder.Default
    @Column(name = "completed")
    private Boolean completed = false;

    /**
     * Active Flag.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    /**
     * Internal Remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

}