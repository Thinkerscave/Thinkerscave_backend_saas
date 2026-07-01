package com.thinkerscave.audit.entity;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Unified audit log — one row per meaningful business action.
 * Designed for H2 and Postgres (no jsonb — {@code changes} is a TEXT/CLOB).
 */
@Entity
@Table(name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_log_org_time",  columnList = "organization_id, occurred_at"),
                @Index(name = "idx_audit_log_entity",    columnList = "entity_type, entity_id"),
                @Index(name = "idx_audit_log_actor",     columnList = "actor_user_id"),
                @Index(name = "idx_audit_log_event",     columnList = "event_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AuditLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "tenant_code", length = 64)
    private String tenantCode;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private AuditEventType eventType;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "entity_type", length = 128)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Lob
    @Column(name = "changes")
    private String changes;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
