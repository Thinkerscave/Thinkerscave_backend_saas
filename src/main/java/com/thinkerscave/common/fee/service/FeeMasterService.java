package com.thinkerscave.common.fee.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.enums.GenericStatus;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.fee.domain.FeeGroup;
import com.thinkerscave.common.fee.domain.FeeHead;
import com.thinkerscave.common.fee.domain.FeePolicy;
import com.thinkerscave.common.fee.dto.FeeGroupDTO;
import com.thinkerscave.common.fee.dto.FeeHeadDTO;
import com.thinkerscave.common.fee.dto.FeePolicyDTO;
import com.thinkerscave.common.fee.repository.FeeGroupRepository;
import com.thinkerscave.common.fee.repository.FeeHeadRepository;
import com.thinkerscave.common.fee.repository.FeePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for the three fee-master entities: {@link FeeHead}, {@link FeeGroup},
 * {@link FeePolicy}. Kept in one service because each is a thin master and
 * they are usually edited together in the Fee Setup workspace.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeeMasterService {

    private final FeeHeadRepository feeHeadRepository;
    private final FeeGroupRepository feeGroupRepository;
    private final FeePolicyRepository feePolicyRepository;
    private final AuditPublisher auditPublisher;

    // -------------------------------------------------------- Fee Head ----

    public List<FeeHeadDTO> listFeeHeads() {
        Long orgId = currentOrgId();
        return feeHeadRepository.findByOrganizationId(orgId).stream()
                .map(this::toDto).toList();
    }

    public FeeHeadDTO getFeeHead(Long id) {
        return toDto(loadHead(id));
    }

    @Transactional
    public FeeHeadDTO saveFeeHead(FeeHeadDTO dto) {
        Long orgId = currentOrgId();
        FeeHead head;
        boolean creating = dto.getId() == null;
        if (creating) {
            feeHeadRepository.findByOrganizationIdAndCode(orgId, dto.getCode())
                    .ifPresent(existing -> { throw new ConflictException("Fee head with code '" + dto.getCode() + "' already exists"); });
            head = new FeeHead();
            head.setOrganizationId(orgId);
        } else {
            head = loadHead(dto.getId());
        }
        head.setCode(dto.getCode());
        head.setName(dto.getName());
        head.setDescription(dto.getDescription());
        head.setRefundable(dto.isRefundable());
        head.setTaxable(dto.isTaxable());
        head.setGlCode(dto.getGlCode());
        head.setDisplayOrder(dto.getDisplayOrder());
        head.setStatus(dto.getStatus() != null ? dto.getStatus() : GenericStatus.ACTIVE);
        FeeHead saved = feeHeadRepository.save(head);
        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "fee_head.create" : "fee_head.update",
                "FeeHead", saved.getId(), "Fee head " + saved.getCode());
        return toDto(saved);
    }

    @Transactional
    public void deleteFeeHead(Long id) {
        FeeHead head = loadHead(id);
        feeHeadRepository.delete(head);
        auditPublisher.publish(AuditEventType.DELETE, "fee_head.delete", "FeeHead", id, "Deleted fee head " + head.getCode());
    }

    // ------------------------------------------------------- Fee Group ----

    public List<FeeGroupDTO> listFeeGroups() {
        return feeGroupRepository.findByOrganizationId(currentOrgId()).stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public FeeGroupDTO saveFeeGroup(FeeGroupDTO dto) {
        Long orgId = currentOrgId();
        FeeGroup group;
        boolean creating = dto.getId() == null;
        if (creating) {
            feeGroupRepository.findByOrganizationIdAndCode(orgId, dto.getCode())
                    .ifPresent(e -> { throw new ConflictException("Fee group code '" + dto.getCode() + "' already exists"); });
            group = new FeeGroup();
            group.setOrganizationId(orgId);
        } else {
            group = feeGroupRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fee group not found: " + dto.getId()));
        }
        group.setCode(dto.getCode());
        group.setName(dto.getName());
        group.setDescription(dto.getDescription());
        group.setStatus(dto.getStatus() != null ? dto.getStatus() : GenericStatus.ACTIVE);
        FeeGroup saved = feeGroupRepository.save(group);
        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "fee_group.create" : "fee_group.update",
                "FeeGroup", saved.getId(), "Fee group " + saved.getCode());
        return toDto(saved);
    }

    @Transactional
    public void deleteFeeGroup(Long id) {
        feeGroupRepository.deleteById(id);
        auditPublisher.publish(AuditEventType.DELETE, "fee_group.delete", "FeeGroup", id, "Deleted fee group");
    }

    // ------------------------------------------------------ Fee Policy ----

    public List<FeePolicyDTO> listFeePolicies() {
        return feePolicyRepository.findByOrganizationId(currentOrgId()).stream()
                .map(this::toDto).toList();
    }

    public FeePolicyDTO getFeePolicy(Long id) {
        return toDto(loadPolicy(id));
    }

    @Transactional
    public FeePolicyDTO saveFeePolicy(FeePolicyDTO dto) {
        Long orgId = currentOrgId();
        FeePolicy policy;
        boolean creating = dto.getId() == null;
        if (creating) {
            feePolicyRepository.findByOrganizationIdAndCode(orgId, dto.getCode())
                    .ifPresent(e -> { throw new ConflictException("Fee policy code '" + dto.getCode() + "' already exists"); });
            policy = new FeePolicy();
            policy.setOrganizationId(orgId);
        } else {
            policy = loadPolicy(dto.getId());
        }
        policy.setCode(dto.getCode());
        policy.setName(dto.getName());
        policy.setGracePeriodDays(dto.getGracePeriodDays());
        policy.setLateFeeAmount(dto.getLateFeeAmount());
        policy.setLateFeePercent(dto.getLateFeePercent());
        policy.setCompoundingDays(dto.getCompoundingDays());
        policy.setMaxLateFee(dto.getMaxLateFee());
        policy.setEarlyBirdDiscountPercent(dto.getEarlyBirdDiscountPercent());
        policy.setEarlyBirdCutoffDays(dto.getEarlyBirdCutoffDays());
        policy.setReminderIntervalsCsv(dto.getReminderIntervalsCsv());
        policy.setActive(dto.isActive());
        FeePolicy saved = feePolicyRepository.save(policy);
        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "fee_policy.create" : "fee_policy.update",
                "FeePolicy", saved.getId(), "Fee policy " + saved.getCode());
        return toDto(saved);
    }

    @Transactional
    public void deleteFeePolicy(Long id) {
        feePolicyRepository.deleteById(id);
        auditPublisher.publish(AuditEventType.DELETE, "fee_policy.delete", "FeePolicy", id, "Deleted fee policy");
    }

    // ----------------------------------------------------------- helpers --

    private Long currentOrgId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            throw new BadRequestException("No organization context resolved for current request");
        }
        return orgId;
    }

    private FeeHead loadHead(Long id) {
        return feeHeadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee head not found: " + id));
    }

    private FeePolicy loadPolicy(Long id) {
        return feePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee policy not found: " + id));
    }

    private FeeHeadDTO toDto(FeeHead e) {
        return FeeHeadDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .refundable(e.isRefundable())
                .taxable(e.isTaxable())
                .glCode(e.getGlCode())
                .displayOrder(e.getDisplayOrder())
                .status(e.getStatus())
                .build();
    }

    private FeeGroupDTO toDto(FeeGroup e) {
        return FeeGroupDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .status(e.getStatus())
                .build();
    }

    private FeePolicyDTO toDto(FeePolicy e) {
        return FeePolicyDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .gracePeriodDays(e.getGracePeriodDays())
                .lateFeeAmount(e.getLateFeeAmount())
                .lateFeePercent(e.getLateFeePercent())
                .compoundingDays(e.getCompoundingDays())
                .maxLateFee(e.getMaxLateFee())
                .earlyBirdDiscountPercent(e.getEarlyBirdDiscountPercent())
                .earlyBirdCutoffDays(e.getEarlyBirdCutoffDays())
                .reminderIntervalsCsv(e.getReminderIntervalsCsv())
                .active(e.isActive())
                .build();
    }
}
