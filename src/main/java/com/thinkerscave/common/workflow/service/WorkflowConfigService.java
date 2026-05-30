package com.thinkerscave.common.workflow.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.enums.GenericStatus;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.workflow.domain.WorkflowConfig;
import com.thinkerscave.common.workflow.dto.WorkflowConfigDTO;
import com.thinkerscave.common.workflow.mapper.WorkflowMapper;
import com.thinkerscave.common.workflow.repository.WorkflowConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Per-organization workflow configuration CRUD plus a small lookup helper
 * used by approval flows (admission/refund/transfer).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WorkflowConfigService {

    private final WorkflowConfigRepository workflowRepository;
    private final AuditPublisher auditPublisher;
    private final WorkflowMapper workflowMapper;

    public List<WorkflowConfigDTO> list() {
        return workflowRepository.findByOrganizationId(currentOrgId())
                .stream().map(workflowMapper::toDto).toList();
    }

    public WorkflowConfigDTO get(Long id) {
        return workflowMapper.toDto(load(id));
    }

    public Optional<WorkflowConfigDTO> findByKey(String workflowKey) {
        return workflowRepository.findByOrganizationIdAndWorkflowKey(currentOrgId(), workflowKey)
                .map(workflowMapper::toDto);
    }

    @Transactional
    public WorkflowConfigDTO save(WorkflowConfigDTO dto) {
        if (dto.getWorkflowKey() == null || dto.getWorkflowKey().isBlank()) {
            throw new BadRequestException("workflowKey is required");
        }
        if (dto.getDisplayName() == null || dto.getDisplayName().isBlank()) {
            throw new BadRequestException("displayName is required");
        }
        Long orgId = currentOrgId();
        WorkflowConfig w;
        boolean creating;
        if (dto.getId() != null) {
            w = load(dto.getId());
            creating = false;
            if (!w.getWorkflowKey().equals(dto.getWorkflowKey())) {
                final Long currentId = w.getId();
                workflowRepository.findByOrganizationIdAndWorkflowKey(orgId, dto.getWorkflowKey())
                        .ifPresent(other -> {
                            if (!other.getId().equals(currentId)) {
                                throw new ConflictException("Workflow key already exists: " + dto.getWorkflowKey());
                            }
                        });
                w.setWorkflowKey(dto.getWorkflowKey());
            }
        } else {
            workflowRepository.findByOrganizationIdAndWorkflowKey(orgId, dto.getWorkflowKey())
                    .ifPresent(x -> { throw new ConflictException("Workflow key already exists: " + dto.getWorkflowKey()); });
            w = WorkflowConfig.builder().workflowKey(dto.getWorkflowKey()).build();
            w.setOrganizationId(orgId);
            creating = true;
        }
        w.setDisplayName(dto.getDisplayName());
        w.setDescription(dto.getDescription());
        w.setRequireApproval(dto.isRequireApproval());
        w.setApprovalLevels(dto.getApprovalLevels());
        w.setApproverRoleCodes(dto.getApproverRoleCodes());
        w.setAutoCloseAfterDays(dto.getAutoCloseAfterDays());
        w.setStatus(dto.getStatus() != null ? dto.getStatus() : GenericStatus.ACTIVE);
        w = workflowRepository.save(w);
        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.CONFIG_CHANGE,
                creating ? "WORKFLOW_CONFIG_CREATE" : "WORKFLOW_CONFIG_UPDATE",
                "WorkflowConfig", w.getId(),
                "Workflow config " + w.getWorkflowKey() + (creating ? " created" : " updated"));
        return workflowMapper.toDto(w);
    }

    @Transactional
    public void delete(Long id) {
        WorkflowConfig w = load(id);
        workflowRepository.delete(w);
        auditPublisher.publish(AuditEventType.DELETE, "WORKFLOW_CONFIG_DELETE",
                "WorkflowConfig", id, "Workflow config " + w.getWorkflowKey() + " deleted");
    }

    private WorkflowConfig load(Long id) {
        WorkflowConfig w = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow config not found: " + id));
        Long orgId = currentOrgId();
        if (orgId != null && !orgId.equals(w.getOrganizationId())) {
            throw new ResourceNotFoundException("Workflow config not found: " + id);
        }
        return w;
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
