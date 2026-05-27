package com.thinkerscave.common.communication.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Direct-message thread between a small group of users (e.g. teacher–parent,
 * admin–staff). Individual messages live in {@link Message}.
 */
@Entity
@Table(name = "message_thread",
        indexes = {
                @Index(name = "idx_msg_thread_org",  columnList = "organization_id"),
                @Index(name = "idx_msg_thread_last", columnList = "last_message_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageThread extends OrganizationScopedEntity {

    @Column(name = "subject", length = 256)
    private String subject;

    /** Comma-separated user ids — keeps the model lightweight for scaffold phase. */
    @Column(name = "participant_user_ids_csv", nullable = false, length = 500)
    private String participantUserIdsCsv;

    @Column(name = "context_ref", length = 128)
    private String contextRef;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_by_user_id")
    private Long lastMessageByUserId;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;
}
