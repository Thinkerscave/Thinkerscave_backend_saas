package com.thinkerscave.audit.dto;

import com.thinkerscave.audit.enums.SeverityLevel;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityAuditLogDTO {
    private Long id;
    private String eventCode;
    private String username;
    private String tenantCode;
    private String sourceIp;
    private String userAgent;
    private boolean success;
    private SeverityLevel severity;
    private String message;
    private String correlationId;
    private Instant occurredAt;
}
