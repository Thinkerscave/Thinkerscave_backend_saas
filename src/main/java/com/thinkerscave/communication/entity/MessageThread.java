package com.thinkerscave.communication.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "message_thread",
        indexes = {
            @Index(name = "idx_thread_org", columnList = "organization_id"),
            @Index(name = "idx_thread_last_msg", columnList = "last_message_at")
        })
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class MessageThread extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "thread_id")
    private Long threadId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "participant_user_ids_csv", length = 2000)
    private String participantUserIdsCsv;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "closed")
    private boolean closed = false;
}
