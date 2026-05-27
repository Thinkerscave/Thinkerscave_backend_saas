package com.thinkerscave.common.communication.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A single message inside a {@link MessageThread}. */
@Entity
@Table(name = "message",
        indexes = {
                @Index(name = "idx_message_thread", columnList = "message_thread_id"),
                @Index(name = "idx_message_sender", columnList = "sender_user_id"),
                @Index(name = "idx_message_sent",   columnList = "sent_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends AuditableBaseEntity {

    @Column(name = "message_thread_id", nullable = false)
    private Long messageThreadId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Lob
    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
