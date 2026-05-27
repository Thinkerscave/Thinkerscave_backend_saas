package com.thinkerscave.common.audit.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.domain.AuditLog;
import com.thinkerscave.common.audit.repository.AuditLogRepository;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Lightweight, fire-and-forget façade for writing {@link AuditLog} rows.
 * Services should call {@link #publish} rather than touching the repository
 * directly so the cross-cutting concern stays uniform across modules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditPublisher {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void publish(AuditEventType eventType,
                        String action,
                        String entityType,
                        Object entityId,
                        String summary,
                        String changesJson) {
        try {
            AuditLog event = AuditLog.builder()
                    .eventType(eventType)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId == null ? null : String.valueOf(entityId))
                    .summary(summary)
                    .changes(changesJson)
                    .actorUsername(SecurityUtil.getCurrentUsername())
                    .occurredAt(Instant.now())
                    .correlationId(MDC.get("correlationId"))
                    .tenantCode(MDC.get("tenantId"))
                    .build();
            Long orgId = OrganizationContext.getOrganizationId();
            if (orgId != null) {
                event.setOrganizationId(orgId);
            }
            auditLogRepository.save(event);
        } catch (Exception ex) {
            // Audit logging must never fail the calling transaction.
            log.warn("Failed to write audit event {} for {}#{}: {}",
                    eventType, entityType, entityId, ex.getMessage());
        }
    }

    public void publish(AuditEventType eventType, String action,
                        String entityType, Object entityId, String summary) {
        publish(eventType, action, entityType, entityId, summary, null);
    }
}
