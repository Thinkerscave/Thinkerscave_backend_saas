package com.thinkerscave.common.communication.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.common.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Outbound notification — one row per logical message, with per-recipient
 * dispatch attempts tracked in {@link NotificationRecipient}.
 */
@Entity
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_notification_org",    columnList = "organization_id"),
                @Index(name = "idx_notification_status", columnList = "status"),
                @Index(name = "idx_notification_sched",  columnList = "scheduled_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends OrganizationScopedEntity {

    @Column(name = "subject", nullable = false, length = 256)
    private String subject;

    @Lob
    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "channels_csv", nullable = false, length = 128)
    private String channelsCsv;

    @Column(name = "category", length = 64)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 16)
    private SeverityLevel severity;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "triggered_by_user_id")
    private Long triggeredByUserId;

    /** Optional reference back to the source event (e.g. {@code INVOICE:123}). */
    @Column(name = "source_ref", length = 128)
    private String sourceRef;

    @Column(name = "total_recipients")
    private Integer totalRecipients;

    @Column(name = "delivered_count")
    private Integer deliveredCount;

    @Column(name = "failed_count")
    private Integer failedCount;
}
