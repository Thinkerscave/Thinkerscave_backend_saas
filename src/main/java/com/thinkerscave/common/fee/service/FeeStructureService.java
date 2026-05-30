package com.thinkerscave.common.fee.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.enums.GenericStatus;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.fee.domain.FeeStructure;
import com.thinkerscave.common.fee.domain.FeeStructureItem;
import com.thinkerscave.common.fee.dto.FeeStructureDTO;
import com.thinkerscave.common.fee.dto.FeeStructureItemDTO;
import com.thinkerscave.common.fee.mapper.FeeMapper;
import com.thinkerscave.common.fee.repository.FeeStructureItemRepository;
import com.thinkerscave.common.fee.repository.FeeStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregate service for a {@link FeeStructure} and its line items
 * ({@link FeeStructureItem}). Structure + items are saved transactionally as
 * a single graph; items are fully replaced on update (simpler than diffing
 * for an internal config screen).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final FeeStructureItemRepository feeStructureItemRepository;
    private final AuditPublisher auditPublisher;
    private final FeeMapper feeMapper;

    public List<FeeStructureDTO> listByYear(Long academicYearId) {
        return feeStructureRepository
                .findByOrganizationIdAndAcademicYearId(currentOrgId(), academicYearId)
                .stream().map(this::toDtoWithItems).toList();
    }

    public FeeStructureDTO get(Long id) {
        return toDtoWithItems(load(id));
    }

    @Transactional
    public FeeStructureDTO save(FeeStructureDTO dto) {
        Long orgId = currentOrgId();
        FeeStructure entity;
        boolean creating = dto.getId() == null;
        if (creating) {
            entity = new FeeStructure();
            entity.setOrganizationId(orgId);
        } else {
            entity = load(dto.getId());
        }
        entity.setName(dto.getName());
        entity.setAcademicYearId(dto.getAcademicYearId());
        entity.setClassId(dto.getClassId());
        entity.setSectionId(dto.getSectionId());
        entity.setFeePolicyId(dto.getFeePolicyId());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : GenericStatus.ACTIVE);
        entity.setNotes(dto.getNotes());
        FeeStructure saved = feeStructureRepository.save(entity);

        // Replace items (delete then insert)
        if (!creating) {
            feeStructureItemRepository.deleteByFeeStructureId(saved.getId());
        }
        if (dto.getItems() != null) {
            int idx = 0;
            for (FeeStructureItemDTO i : dto.getItems()) {
                FeeStructureItem item = FeeStructureItem.builder()
                        .feeStructureId(saved.getId())
                        .feeHeadId(i.getFeeHeadId())
                        .feeGroupId(i.getFeeGroupId())
                        .amount(i.getAmount())
                        .frequency(i.getFrequency())
                        .dueDayOfMonth(i.getDueDayOfMonth())
                        .optional(i.isOptional())
                        .displayOrder(i.getDisplayOrder() != null ? i.getDisplayOrder() : idx++)
                        .build();
                feeStructureItemRepository.save(item);
            }
        }

        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "fee_structure.create" : "fee_structure.update",
                "FeeStructure", saved.getId(),
                "Fee structure " + saved.getName());
        return toDtoWithItems(saved);
    }

    @Transactional
    public void delete(Long id) {
        FeeStructure entity = load(id);
        feeStructureItemRepository.deleteByFeeStructureId(id);
        feeStructureRepository.delete(entity);
        auditPublisher.publish(AuditEventType.DELETE, "fee_structure.delete",
                "FeeStructure", id, "Deleted fee structure " + entity.getName());
    }

    /** Total annual amount = SUM(item.amount * occurrences_per_year). */
    public BigDecimal computeAnnualTotal(Long feeStructureId) {
        return feeStructureItemRepository.findByFeeStructureId(feeStructureId).stream()
                .map(item -> item.getAmount() == null ? BigDecimal.ZERO :
                        item.getAmount().multiply(BigDecimal.valueOf(occurrencesPerYear(item))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int occurrencesPerYear(FeeStructureItem item) {
        if (item.getFrequency() == null) return 1;
        return switch (item.getFrequency()) {
            case ONE_TIME, ANNUAL -> 1;
            case HALF_YEARLY -> 2;
            case TERM -> 3;
            case QUARTERLY -> 4;
            case MONTHLY -> 12;
        };
    }

    // ---------------------------------------------------------- helpers ---

    private Long currentOrgId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) throw new BadRequestException("No organization context");
        return orgId;
    }

    private FeeStructure load(Long id) {
        return feeStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found: " + id));
    }

    private FeeStructureDTO toDtoWithItems(FeeStructure e) {
        List<FeeStructureItem> items = feeStructureItemRepository.findByFeeStructureId(e.getId());
        FeeStructureDTO dto = feeMapper.toFeeStructureDto(e);
        dto.setItems(feeMapper.toItemDtoList(items));
        return dto;
    }
}
