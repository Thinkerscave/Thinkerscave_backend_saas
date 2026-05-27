package com.thinkerscave.common.audit.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.domain.AuditLog;
import com.thinkerscave.common.audit.domain.SecurityAuditLog;
import com.thinkerscave.common.audit.dto.AuditLogDTO;
import com.thinkerscave.common.audit.dto.SecurityAuditLogDTO;
import com.thinkerscave.common.audit.repository.AuditLogRepository;
import com.thinkerscave.common.audit.repository.SecurityAuditLogRepository;
import com.thinkerscave.common.context.OrganizationContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only query side of the audit subsystem — filtered paged listing of
 * {@link AuditLog} and {@link SecurityAuditLog}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;

    public Page<AuditLogDTO> searchAudit(AuditEventType eventType,
                                         String entityType,
                                         String entityId,
                                         Long actorUserId,
                                         Instant from,
                                         Instant to,
                                         Pageable pageable) {
        Long orgId = currentOrgId();
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (orgId != null) p.add(cb.equal(root.get("organizationId"), orgId));
            if (eventType != null) p.add(cb.equal(root.get("eventType"), eventType));
            if (entityType != null && !entityType.isBlank()) p.add(cb.equal(root.get("entityType"), entityType));
            if (entityId != null && !entityId.isBlank()) p.add(cb.equal(root.get("entityId"), entityId));
            if (actorUserId != null) p.add(cb.equal(root.get("actorUserId"), actorUserId));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return auditLogRepository.findAll(spec, pageable).map(this::toDto);
    }

    public Page<AuditLogDTO> entityHistory(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable).map(this::toDto);
    }

    public Page<SecurityAuditLogDTO> searchSecurity(String username,
                                                    String eventCode,
                                                    Boolean success,
                                                    Instant from,
                                                    Instant to,
                                                    Pageable pageable) {
        Specification<SecurityAuditLog> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (username != null && !username.isBlank()) p.add(cb.equal(root.get("username"), username));
            if (eventCode != null && !eventCode.isBlank()) p.add(cb.equal(root.get("eventCode"), eventCode));
            if (success != null) p.add(cb.equal(root.get("success"), success));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return securityAuditLogRepository.findAll(spec, pageable).map(this::toDto);
    }

    private AuditLogDTO toDto(AuditLog a) {
        return AuditLogDTO.builder()
                .id(a.getId())
                .organizationId(a.getOrganizationId())
                .tenantCode(a.getTenantCode())
                .correlationId(a.getCorrelationId())
                .eventType(a.getEventType())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .actorUserId(a.getActorUserId())
                .actorUsername(a.getActorUsername())
                .sourceIp(a.getSourceIp())
                .userAgent(a.getUserAgent())
                .changes(a.getChanges())
                .summary(a.getSummary())
                .occurredAt(a.getOccurredAt())
                .build();
    }

    private SecurityAuditLogDTO toDto(SecurityAuditLog s) {
        return SecurityAuditLogDTO.builder()
                .id(s.getId())
                .eventCode(s.getEventCode())
                .username(s.getUsername())
                .tenantCode(s.getTenantCode())
                .sourceIp(s.getSourceIp())
                .userAgent(s.getUserAgent())
                .success(s.isSuccess())
                .severity(s.getSeverity())
                .message(s.getMessage())
                .correlationId(s.getCorrelationId())
                .occurredAt(s.getOccurredAt())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
