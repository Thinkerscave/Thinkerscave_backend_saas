package com.thinkerscave.audit.service;

import com.thinkerscave.audit.dto.AuditLogDTO;
import com.thinkerscave.audit.dto.SecurityAuditLogDTO;
import com.thinkerscave.audit.entity.AuditLog;
import com.thinkerscave.audit.entity.SecurityAuditLog;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.repository.AuditLogRepository;
import com.thinkerscave.audit.repository.SecurityAuditLogRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.util.PageRequestUtil;
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
 * Read-only query service for audit logs.
 * All list operations are org-scoped via {@link OrganizationContext}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;

    public PageResponse<AuditLogDTO> searchAudit(AuditEventType eventType,
                                                  String entityType,
                                                  String entityId,
                                                  Long actorUserId,
                                                  Instant from,
                                                  Instant to,
                                                  Integer page,
                                                  Integer size,
                                                  String sort) {
        Long orgId = OrganizationContext.getOrganizationId();
        Pageable pageable = PageRequestUtil.of(page, size, sort);

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (orgId != null)      predicates.add(cb.equal(root.get("organizationId"), orgId));
            if (eventType != null)  predicates.add(cb.equal(root.get("eventType"), eventType));
            if (entityType != null && !entityType.isBlank()) predicates.add(cb.equal(root.get("entityType"), entityType));
            if (entityId != null && !entityId.isBlank())     predicates.add(cb.equal(root.get("entityId"), entityId));
            if (actorUserId != null) predicates.add(cb.equal(root.get("actorUserId"), actorUserId));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null)   predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(auditLogRepository.findAll(spec, pageable), this::toDto);
    }

    public PageResponse<AuditLogDTO> entityHistory(String entityType, String entityId,
                                                    Integer page, Integer size, String sort) {
        Pageable pageable = PageRequestUtil.of(page, size, sort);
        return PageResponse.of(
                auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable),
                this::toDto);
    }

    public PageResponse<SecurityAuditLogDTO> searchSecurity(String username,
                                                             String eventCode,
                                                             Boolean success,
                                                             Instant from,
                                                             Instant to,
                                                             Integer page,
                                                             Integer size,
                                                             String sort) {
        Pageable pageable = PageRequestUtil.of(page, size, sort);
        Specification<SecurityAuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null && !username.isBlank()) predicates.add(cb.equal(root.get("username"), username));
            if (eventCode != null && !eventCode.isBlank()) predicates.add(cb.equal(root.get("eventCode"), eventCode));
            if (success != null) predicates.add(cb.equal(root.get("success"), success));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null)   predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.of(securityAuditLogRepository.findAll(spec, pageable), this::toSecDto);
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

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
                .changes(a.getChanges())
                .summary(a.getSummary())
                .occurredAt(a.getOccurredAt())
                .build();
    }

    private SecurityAuditLogDTO toSecDto(SecurityAuditLog s) {
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
}
