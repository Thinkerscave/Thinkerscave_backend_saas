package com.thinkerscave.communication.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "message",
        indexes = {
            @Index(name = "idx_message_thread", columnList = "message_thread_id, sent_at"),
            @Index(name = "idx_message_sender", columnList = "sender_user_id")
        })
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Message extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "message_id")
    private Long messageId;

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

    @Column(name = "deleted")
    private boolean deleted = false;
}
