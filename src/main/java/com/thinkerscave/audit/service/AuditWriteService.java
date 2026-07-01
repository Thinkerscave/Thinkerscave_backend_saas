package com.thinkerscave.audit.service;

import com.thinkerscave.audit.entity.AuditLog;
import com.thinkerscave.audit.entity.SecurityAuditLog;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.enums.SeverityLevel;
import com.thinkerscave.audit.repository.AuditLogRepository;
import com.thinkerscave.audit.repository.SecurityAuditLogRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Write side of the audit subsystem.
 * All operations run in a SEPARATE transaction (propagation REQUIRES_NEW) so
 * an audit entry is always persisted even if the caller transaction rolls back.
 * Calls are also async so they don't block the main request thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditWriteService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEventType eventType,
                       String action,
                       String entityType,
                       String entityId,
                       String summary) {
        try {
            String actor = currentUsername();
            AuditLog entry = AuditLog.builder()
                    .organizationId(OrganizationContext.getOrganizationId())
                    .correlationId(MDC.get("correlationId"))
                    .eventType(eventType)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .actorUsername(actor)
                    .summary(summary)
                    .occurredAt(Instant.now())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Audit write failed: {}", e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSecurity(String eventCode,
                                String username,
                                boolean success,
                                SeverityLevel severity,
                                String message) {
        try {
            SecurityAuditLog entry = SecurityAuditLog.builder()
                    .eventCode(eventCode)
                    .username(username)
                    .success(success)
                    .severity(severity)
                    .message(message)
                    .correlationId(MDC.get("correlationId"))
                    .occurredAt(Instant.now())
                    .build();
            securityAuditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Security audit write failed: {}", e.getMessage());
        }
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }
}
