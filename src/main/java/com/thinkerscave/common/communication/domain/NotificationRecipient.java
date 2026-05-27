package com.thinkerscave.common.communication.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Per-channel per-recipient dispatch row of a {@link Notification}. */
@Entity
@Table(name = "notification_recipient",
        indexes = {
                @Index(name = "idx_notif_recip_notif", columnList = "notification_id"),
                @Index(name = "idx_notif_recip_user",  columnList = "user_id"),
                @Index(name = "idx_notif_recip_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecipient extends AuditableBaseEntity {

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "user_id")
    private Long userId;

    /** Email / phone / device token — depends on {@link #channel}. */
    @Column(name = "address", nullable = false, length = 256)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;
}
