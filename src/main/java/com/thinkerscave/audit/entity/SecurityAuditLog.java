package com.thinkerscave.audit.entity;

import com.thinkerscave.audit.enums.SeverityLevel;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Security-specific audit log: login attempts, lockouts, permission denials,
 * password resets, token issuance / revocation.
 */
@Entity
@Table(name = "security_audit_log",
        indexes = {
                @Index(name = "idx_sec_audit_time",     columnList = "occurred_at"),
                @Index(name = "idx_sec_audit_severity", columnList = "severity"),
                @Index(name = "idx_sec_audit_user",     columnList = "username")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SecurityAuditLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    @Column(name = "event_code", nullable = false, length = 64)
    private String eventCode;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "tenant_code", length = 64)
    private String tenantCode;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private SeverityLevel severity;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
