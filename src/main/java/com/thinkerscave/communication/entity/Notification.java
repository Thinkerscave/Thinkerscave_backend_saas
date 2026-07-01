package com.thinkerscave.communication.entity;

import com.thinkerscave.communication.enums.NotificationStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notification",
        indexes = {
            @Index(name = "idx_notification_org_status", columnList = "organization_id, status"),
            @Index(name = "idx_notification_scheduled", columnList = "scheduled_at")
        })
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Notification extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Lob
    @Column(name = "body")
    private String body;

    @Column(name = "channels_csv", length = 100)
    private String channelsCsv;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "triggered_by_user_id")
    private Long triggeredByUserId;

    @Column(name = "total_recipients")
    private Integer totalRecipients = 0;

    @Column(name = "delivered_count")
    private Integer deliveredCount = 0;

    @Column(name = "failed_count")
    private Integer failedCount = 0;
}
