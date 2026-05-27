package com.thinkerscave.common.audit.dto;

import com.thinkerscave.common.audit.domain.AuditEventType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    private Long id;
    private Long organizationId;
    private String tenantCode;
    private String correlationId;
    private AuditEventType eventType;
    private String action;
    private String entityType;
    private String entityId;
    private Long actorUserId;
    private String actorUsername;
    private String sourceIp;
    private String userAgent;
    private String changes;
    private String summary;
    private Instant occurredAt;
}
