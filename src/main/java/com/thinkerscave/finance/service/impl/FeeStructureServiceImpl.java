package com.thinkerscave.finance.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.dto.request.CloneFeeStructureRequest;
import com.thinkerscave.finance.dto.request.FeeStructureItemRequest;
import com.thinkerscave.finance.dto.request.FeeStructureRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.ClonePreviewResponse;
import com.thinkerscave.finance.dto.response.FeeStructureItemResponse;
import com.thinkerscave.finance.dto.response.FeeStructureResponse;
import com.thinkerscave.finance.entity.FeeHead;
import com.thinkerscave.finance.entity.FeeStructure;
import com.thinkerscave.finance.entity.FeeStructureItem;
import com.thinkerscave.finance.enums.FeeFrequency;
import com.thinkerscave.finance.enums.FeeItemType;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.enums.FeeServiceKey;
import com.thinkerscave.finance.repository.FeeHeadRepository;
import com.thinkerscave.finance.repository.FeeStructureItemRepository;
import com.thinkerscave.finance.repository.FeeStructureRepository;
import com.thinkerscave.finance.repository.StudentBillingPeriodRepository;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.service.FeeStructureService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeStructureServiceImpl implements FeeStructureService {

    private final FeeStructureRepository structureRepository;
    private final FeeStructureItemRepository itemRepository;
    private final FeeHeadRepository feeHeadRepository;
    private final StudentBillingPeriodRepository billingPeriodRepository;
    private final FinanceAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    public PageResponse<FeeStructureResponse> list(String q, Long academicYearId, Long classId,
                                                   FeeMasterStatus status, Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);
        String qq = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(structureRepository.search(qq, academicYearId, classId, status, pageable), this::toResponse);
    }

    @Override
    public FeeStructureResponse get(Long id) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return toResponse(require(id));
    }

    @Override
    @Transactional
    public FeeStructureResponse create(FeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        FeeMasterStatus status = request.getStatus() == null ? FeeMasterStatus.ACTIVE : request.getStatus();
        if (status == FeeMasterStatus.ACTIVE) {
            structureRepository.findByAcademicYearIdAndClassIdAndStatus(
                    request.getAcademicYearId(), request.getClassId(), FeeMasterStatus.ACTIVE)
                    .ifPresent(s -> {
                        throw new AlreadyExistsException("ACTIVE fee structure already exists for year/class");
                    });
        }
        FeeStructure structure = new FeeStructure();
        structure.setName(request.getName().trim());
        structure.setAcademicYearId(request.getAcademicYearId());
        structure.setClassId(request.getClassId());
        structure.setDueDay(request.getDueDay());
        structure.setStatus(status);
        FeeStructure saved = structureRepository.save(structure);
        saveItems(saved.getFeeStructureId(), request.getItems(), true);
        auditWriteService.record(AuditEventType.CREATE, "FEE_STRUCTURE_CREATE", "FeeStructure",
                String.valueOf(saved.getFeeStructureId()), "Created fee structure");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FeeStructureResponse update(Long id, FeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        FeeStructure structure = require(id);
        structure.setName(request.getName().trim());
        structure.setAcademicYearId(request.getAcademicYearId());
        structure.setClassId(request.getClassId());
        structure.setDueDay(request.getDueDay());
        if (request.getStatus() != null) {
            structure.setStatus(request.getStatus());
        }
        if (structure.getStatus() == FeeMasterStatus.ACTIVE) {
            structureRepository.findByAcademicYearIdAndClassIdAndStatus(
                    structure.getAcademicYearId(), structure.getClassId(), FeeMasterStatus.ACTIVE)
                    .ifPresent(existing -> {
                        if (!existing.getFeeStructureId().equals(id)) {
                            throw new AlreadyExistsException("ACTIVE fee structure already exists for year/class");
                        }
                    });
        }
        structureRepository.save(structure);
        itemRepository.deleteByFeeStructureId(id);
        saveItems(id, request.getItems(), false);
        auditWriteService.record(AuditEventType.UPDATE, "FEE_STRUCTURE_UPDATE", "FeeStructure",
                String.valueOf(id), "Updated fee structure (dues not rewritten)");
        return toResponse(structure);
    }

    @Override
    @Transactional
    public FeeStructureResponse updateStatus(Long id, StatusUpdateRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        FeeStructure structure = require(id);
        if (request.getStatus() == FeeMasterStatus.ACTIVE) {
            structureRepository.findByAcademicYearIdAndClassIdAndStatus(
                    structure.getAcademicYearId(), structure.getClassId(), FeeMasterStatus.ACTIVE)
                    .ifPresent(existing -> {
                        if (!existing.getFeeStructureId().equals(id)) {
                            throw new AlreadyExistsException("Another ACTIVE structure exists for year/class");
                        }
                    });
        }
        structure.setStatus(request.getStatus());
        structureRepository.save(structure);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "FEE_STRUCTURE_STATUS", "FeeStructure",
                String.valueOf(id), "Status -> " + request.getStatus());
        return toResponse(structure);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        FeeStructure structure = require(id);
        if (billingPeriodRepository.existsByFeeStructureId(id)) {
            throw new BusinessException("Cannot delete fee structure referenced by billing periods");
        }
        itemRepository.deleteByFeeStructureId(id);
        structureRepository.delete(structure);
        auditWriteService.record(AuditEventType.DELETE, "FEE_STRUCTURE_DELETE", "FeeStructure",
                String.valueOf(id), "Deleted fee structure");
    }

    @Override
    public ClonePreviewResponse clonePreview(Long id, CloneFeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        FeeStructure source = require(id);
        List<ClonePreviewResponse.CloneClassPreview> classes = new ArrayList<>();
        for (CloneFeeStructureRequest.CloneTarget target : request.getTargets()) {
            var existing = structureRepository.findByAcademicYearIdAndClassIdAndStatus(
                    request.getTargetAcademicYearId(), target.getClassId(), FeeMasterStatus.ACTIVE);
            ClonePreviewResponse.Conflict conflict = existing.map(s -> ClonePreviewResponse.Conflict.builder()
                    .classId(target.getClassId())
                    .existingStructureId(s.getFeeStructureId())
                    .existingName(s.getName())
                    .build()).orElse(null);
            classes.add(ClonePreviewResponse.CloneClassPreview.builder()
                    .classId(target.getClassId())
                    .proposedName(target.getName() != null ? target.getName() : source.getName())
                    .dueDay(target.getDueDay() != null ? target.getDueDay() : source.getDueDay())
                    .conflict(conflict)
                    .build());
        }
        return ClonePreviewResponse.builder().classes(classes).build();
    }

    @Override
    @Transactional
    public List<FeeStructureResponse> clone(Long id, CloneFeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        FeeStructure source = require(id);
        List<FeeStructureItem> sourceItems = itemRepository.findByFeeStructureIdOrderByFeeStructureItemIdAsc(id);
        List<FeeStructureResponse> created = new ArrayList<>();

        for (CloneFeeStructureRequest.CloneTarget target : request.getTargets()) {
            var existingOpt = structureRepository.findByAcademicYearIdAndClassIdAndStatus(
                    request.getTargetAcademicYearId(), target.getClassId(), FeeMasterStatus.ACTIVE);
            if (existingOpt.isPresent()) {
                if (request.getOnConflict() == CloneFeeStructureRequest.OnConflict.CANCEL_CLASS) {
                    continue;
                }
                // REPLACE_DEACTIVATE: one TX lock → deactivate → create
                FeeStructure existing = structureRepository.findByIdForUpdate(existingOpt.get().getFeeStructureId())
                        .orElseThrow(() -> new ResourceNotFoundException("Structure not found for lock"));
                existing.setStatus(FeeMasterStatus.INACTIVE);
                structureRepository.save(existing);
            }

            FeeStructure clone = new FeeStructure();
            clone.setName(target.getName() != null ? target.getName().trim() : source.getName());
            clone.setAcademicYearId(request.getTargetAcademicYearId());
            clone.setClassId(target.getClassId());
            clone.setDueDay(target.getDueDay() != null ? target.getDueDay() : source.getDueDay());
            clone.setStatus(FeeMasterStatus.ACTIVE);
            FeeStructure saved = structureRepository.save(clone);

            List<FeeStructureItemRequest> itemReqs = target.getItems();
            if (itemReqs != null && !itemReqs.isEmpty()) {
                saveItems(saved.getFeeStructureId(), itemReqs, false);
            } else {
                for (FeeStructureItem si : sourceItems) {
                    FeeStructureItem ni = new FeeStructureItem();
                    ni.setFeeStructureId(saved.getFeeStructureId());
                    ni.setFeeHeadId(si.getFeeHeadId());
                    ni.setAmount(si.getAmount());
                    ni.setFrequency(si.getFrequency());
                    ni.setType(si.getType());
                    ni.setServiceKey(si.getServiceKey());
                    itemRepository.save(ni);
                }
            }
            created.add(toResponse(saved));
        }
        auditWriteService.record(AuditEventType.CREATE, "FEE_STRUCTURE_CLONE", "FeeStructure",
                String.valueOf(id), "Cloned fee structure to " + created.size() + " class(es)");
        return created;
    }

    private void saveItems(Long structureId, List<FeeStructureItemRequest> items, boolean requireActiveHead) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Fee structure requires at least one item");
        }
        Set<Long> heads = new HashSet<>();
        for (FeeStructureItemRequest item : items) {
            if (!heads.add(item.getFeeHeadId())) {
                throw new BadRequestException("Duplicate fee head in structure");
            }
            FeeHead head = feeHeadRepository.findById(item.getFeeHeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fee head not found: " + item.getFeeHeadId()));
            if (requireActiveHead && head.getStatus() != FeeMasterStatus.ACTIVE) {
                throw new BadRequestException("Fee head must be ACTIVE: " + head.getName());
            }
            FeeServiceKey serviceKey = item.getServiceKey() == null ? FeeServiceKey.NONE : item.getServiceKey();
            if (item.getType() == FeeItemType.MANDATORY && serviceKey != FeeServiceKey.NONE) {
                throw new BadRequestException("MANDATORY items must use serviceKey NONE");
            }
            if (item.getType() == FeeItemType.OPTIONAL
                    && serviceKey != FeeServiceKey.TRANSPORT && serviceKey != FeeServiceKey.HOSTEL) {
                throw new BadRequestException("OPTIONAL items require TRANSPORT or HOSTEL");
            }
            FeeStructureItem entity = new FeeStructureItem();
            entity.setFeeStructureId(structureId);
            entity.setFeeHeadId(item.getFeeHeadId());
            entity.setAmount(item.getAmount());
            entity.setFrequency(item.getFrequency());
            entity.setType(item.getType());
            entity.setServiceKey(serviceKey);
            itemRepository.save(entity);
        }
    }

    private FeeStructure require(Long id) {
        return structureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found"));
    }

    private FeeStructureResponse toResponse(FeeStructure s) {
        List<FeeStructureItem> items = itemRepository.findByFeeStructureIdOrderByFeeStructureItemIdAsc(s.getFeeStructureId());
        BigDecimal mandatory = BigDecimal.ZERO;
        BigDecimal optional = BigDecimal.ZERO;
        boolean allMonthly = !items.isEmpty();
        List<FeeStructureItemResponse> itemResponses = new ArrayList<>();
        for (FeeStructureItem item : items) {
            if (item.getType() == FeeItemType.MANDATORY) {
                mandatory = mandatory.add(item.getAmount());
            } else {
                optional = optional.add(item.getAmount());
            }
            if (item.getFrequency() != FeeFrequency.MONTHLY) {
                allMonthly = false;
            }
            FeeHead head = feeHeadRepository.findById(item.getFeeHeadId()).orElse(null);
            itemResponses.add(FeeStructureItemResponse.builder()
                    .feeStructureItemId(item.getFeeStructureItemId())
                    .feeHeadId(item.getFeeHeadId())
                    .feeHeadName(head != null ? head.getName() : null)
                    .amount(item.getAmount())
                    .frequency(item.getFrequency())
                    .type(item.getType())
                    .serviceKey(item.getServiceKey())
                    .build());
        }
        return FeeStructureResponse.builder()
                .feeStructureId(s.getFeeStructureId())
                .name(s.getName())
                .academicYearId(s.getAcademicYearId())
                .classId(s.getClassId())
                .dueDay(s.getDueDay())
                .status(s.getStatus())
                .mandatorySum(mandatory)
                .optionalSum(optional)
                .configuredMonthlyAmount(allMonthly ? mandatory.add(optional) : null)
                .items(itemResponses)
                .build();
    }
}
