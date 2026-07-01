package com.thinkerscave.communication.entity;

import com.thinkerscave.communication.enums.NotificationChannel;
import com.thinkerscave.communication.enums.NotificationStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notification_recipient",
        indexes = {
            @Index(name = "idx_notif_recipient_notif", columnList = "notification_id"),
            @Index(name = "idx_notif_recipient_user", columnList = "user_id, status")
        })
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class NotificationRecipient extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "address", length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "attempt_count")
    private int attemptCount = 0;
}
