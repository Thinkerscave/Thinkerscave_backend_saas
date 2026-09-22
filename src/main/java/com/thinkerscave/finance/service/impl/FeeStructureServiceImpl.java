package com.thinkerscave.finance.service.impl;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.dto.request.CloneFeeStructureRequest;
import com.thinkerscave.finance.dto.request.ConfigureClassFeeStructureRequest;
import com.thinkerscave.finance.dto.request.CopyClassFeeStructureRequest;
import com.thinkerscave.finance.dto.request.FeeStructureItemRequest;
import com.thinkerscave.finance.dto.request.FeeStructureRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.ClassFeeStructureOverviewResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeStructureServiceImpl implements FeeStructureService {

    private static final String YEAR_LOCKED_MESSAGE =
            "This academic year has been published. Fee structures are now locked and cannot be modified.";

    private final FeeStructureRepository structureRepository;
    private final FeeStructureItemRepository itemRepository;
    private final FeeHeadRepository feeHeadRepository;
    private final StudentBillingPeriodRepository billingPeriodRepository;
    private final FinanceAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;
    private final ClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    public PageResponse<FeeStructureResponse> list(String q, Long academicYearId, Long classId,
                                                   FeeMasterStatus status, Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);
        String qq = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(structureRepository.search(qq, academicYearId, classId, status, pageable), this::toResponse);
    }

    @Override
    public PageResponse<ClassFeeStructureOverviewResponse> classOverview(
            Long academicYearId, String q, String configuredFilter, Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);

        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        boolean yearEditable = isYearEditable(year.getStatus());

        List<AcademicClass> classes = classRepository
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(academicYearId);

        if (q != null && !q.isBlank()) {
            String qq = q.trim().toLowerCase(Locale.ROOT);
            classes = classes.stream()
                    .filter(c -> (c.getName() != null && c.getName().toLowerCase(Locale.ROOT).contains(qq))
                            || (c.getCode() != null && c.getCode().toLowerCase(Locale.ROOT).contains(qq)))
                    .toList();
        }

        String filter = configuredFilter == null || configuredFilter.isBlank()
                ? "ALL"
                : configuredFilter.trim().toUpperCase(Locale.ROOT);

        List<ClassFeeStructureOverviewResponse> rows = new ArrayList<>();
        for (AcademicClass academicClass : classes) {
            Optional<FeeStructure> activeOpt = structureRepository.findByAcademicYearIdAndClassIdAndStatus(
                    academicYearId, academicClass.getClassId(), FeeMasterStatus.ACTIVE);
            boolean configured = activeOpt.isPresent();

            if ("CONFIGURED".equals(filter) && !configured) {
                continue;
            }
            if ("NOT_CONFIGURED".equals(filter) && configured) {
                continue;
            }

            if (configured) {
                FeeStructure structure = activeOpt.get();
                FeeTotals totals = computeTotals(
                        itemRepository.findByFeeStructureIdOrderByFeeStructureItemIdAsc(structure.getFeeStructureId()));
                rows.add(ClassFeeStructureOverviewResponse.builder()
                        .classId(academicClass.getClassId())
                        .className(academicClass.getName())
                        .classCode(academicClass.getCode())
                        .configured(true)
                        .feeStructureId(structure.getFeeStructureId())
                        .requiredFees(totals.mandatorySum())
                        .optionalFees(totals.optionalSum())
                        .totalFees(totals.totalFees())
                        .configuredMonthlyAmount(totals.configuredMonthlyAmount())
                        .feeHeadCount(totals.feeHeadCount())
                        .dueDay(structure.getDueDay())
                        .academicYearId(academicYearId)
                        .academicYearName(year.getName())
                        .academicYearStatus(year.getStatus())
                        .yearEditable(yearEditable)
                        .updatedOn(structure.getUpdatedOn())
                        .updatedBy(structure.getUpdatedBy())
                        .build());
            } else {
                rows.add(ClassFeeStructureOverviewResponse.builder()
                        .classId(academicClass.getClassId())
                        .className(academicClass.getName())
                        .classCode(academicClass.getCode())
                        .configured(false)
                        .academicYearId(academicYearId)
                        .academicYearName(year.getName())
                        .academicYearStatus(year.getStatus())
                        .yearEditable(yearEditable)
                        .build());
            }
        }

        long total = rows.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), rows.size());
        List<ClassFeeStructureOverviewResponse> pageContent =
                start >= rows.size() ? List.of() : rows.subList(start, end);
        return PageResponse.of(new PageImpl<>(pageContent, pageable, total));
    }

    @Override
    public FeeStructureResponse get(Long id) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return toResponse(require(id));
    }

    @Override
    public FeeStructureResponse getByClass(Long academicYearId, Long classId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);
        FeeStructure structure = structureRepository
                .findByAcademicYearIdAndClassIdAndStatus(academicYearId, classId, FeeMasterStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fee structure has not been configured for this class"));
        return toResponse(structure);
    }

    @Override
    @Transactional
    public FeeStructureResponse create(FeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        assertYearEditable(request.getAcademicYearId());
        FeeMasterStatus status = request.getStatus() == null ? FeeMasterStatus.ACTIVE : request.getStatus();
        if (status == FeeMasterStatus.ACTIVE) {
            structureRepository.findByAcademicYearIdAndClassIdAndStatus(
                            request.getAcademicYearId(), request.getClassId(), FeeMasterStatus.ACTIVE)
                    .ifPresent(s -> {
                        throw new AlreadyExistsException("ACTIVE fee structure already exists for year/class");
                    });
        }
        FeeStructure structure = new FeeStructure();
        structure.setName(resolveName(request.getName(), request.getAcademicYearId(), request.getClassId()));
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
        assertYearEditable(structure.getAcademicYearId());
        if (!structure.getAcademicYearId().equals(request.getAcademicYearId())) {
            assertYearEditable(request.getAcademicYearId());
        }
        structure.setName(resolveName(request.getName(), request.getAcademicYearId(), request.getClassId()));
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
    public FeeStructureResponse configure(ConfigureClassFeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        assertYearEditable(request.getAcademicYearId());

        AcademicClass academicClass = classRepository.findByIdWithYear(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));
        if (!academicClass.getAcademicYear().getAcademicYearId().equals(request.getAcademicYearId())) {
            throw new BadRequestException("Class does not belong to the academic year");
        }

        Optional<FeeStructure> existingOpt = structureRepository.findByAcademicYearIdAndClassIdAndStatus(
                request.getAcademicYearId(), request.getClassId(), FeeMasterStatus.ACTIVE);

        if (existingOpt.isPresent()) {
            FeeStructure structure = existingOpt.get();
            // Preserve existing due day unless explicitly provided — due day is settings-driven, not edited here.
            if (request.getDueDay() != null) {
                structure.setDueDay(request.getDueDay());
            }
            structureRepository.save(structure);
            itemRepository.deleteByFeeStructureId(structure.getFeeStructureId());
            saveItems(structure.getFeeStructureId(), request.getItems(), false);
            auditWriteService.record(AuditEventType.UPDATE, "FEE_STRUCTURE_UPDATE", "FeeStructure",
                    String.valueOf(structure.getFeeStructureId()), "Configured fee structure (items replaced)");
            return toResponse(structure);
        }

        FeeStructure structure = new FeeStructure();
        structure.setName(resolveName(null, request.getAcademicYearId(), request.getClassId()));
        structure.setAcademicYearId(request.getAcademicYearId());
        structure.setClassId(request.getClassId());
        structure.setDueDay(request.getDueDay() != null ? request.getDueDay() : (short) 10);
        structure.setStatus(FeeMasterStatus.ACTIVE);
        FeeStructure saved = structureRepository.save(structure);
        saveItems(saved.getFeeStructureId(), request.getItems(), true);
        auditWriteService.record(AuditEventType.CREATE, "FEE_STRUCTURE_CREATE", "FeeStructure",
                String.valueOf(saved.getFeeStructureId()), "Configured fee structure");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FeeStructureResponse updateStatus(Long id, StatusUpdateRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        FeeStructure structure = require(id);
        assertYearEditable(structure.getAcademicYearId());
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
        assertYearEditable(structure.getAcademicYearId());
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
                    .proposedName(resolveName(target.getName(), request.getTargetAcademicYearId(), target.getClassId()))
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
        assertYearEditable(request.getTargetAcademicYearId());
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
            clone.setName(resolveName(target.getName(), request.getTargetAcademicYearId(), target.getClassId()));
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

    @Override
    @Transactional
    public List<FeeStructureResponse> copyToClasses(Long id, CopyClassFeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        FeeStructure source = require(id);
        assertYearEditable(source.getAcademicYearId());

        List<FeeStructureItem> sourceItems =
                itemRepository.findByFeeStructureIdOrderByFeeStructureItemIdAsc(id);
        List<FeeStructureResponse> created = new ArrayList<>();

        for (Long targetClassId : request.getTargetClassIds()) {
            if (targetClassId.equals(source.getClassId())) {
                continue;
            }

            AcademicClass targetClass = classRepository.findByIdWithYear(targetClassId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + targetClassId));
            if (!targetClass.getAcademicYear().getAcademicYearId().equals(source.getAcademicYearId())) {
                throw new BadRequestException("Target class must belong to the same academic year");
            }

            var existingOpt = structureRepository.findByAcademicYearIdAndClassIdAndStatus(
                    source.getAcademicYearId(), targetClassId, FeeMasterStatus.ACTIVE);
            if (existingOpt.isPresent()) {
                if (request.getOnConflict() == CopyClassFeeStructureRequest.OnConflict.SKIP) {
                    continue;
                }
                FeeStructure existing = structureRepository.findByIdForUpdate(existingOpt.get().getFeeStructureId())
                        .orElseThrow(() -> new ResourceNotFoundException("Structure not found for lock"));
                existing.setStatus(FeeMasterStatus.INACTIVE);
                structureRepository.save(existing);
            }

            FeeStructure copy = new FeeStructure();
            copy.setName(resolveName(null, source.getAcademicYearId(), targetClassId));
            copy.setAcademicYearId(source.getAcademicYearId());
            copy.setClassId(targetClassId);
            copy.setDueDay(source.getDueDay());
            copy.setStatus(FeeMasterStatus.ACTIVE);
            FeeStructure saved = structureRepository.save(copy);

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
            created.add(toResponse(saved));
        }

        auditWriteService.record(AuditEventType.CREATE, "FEE_STRUCTURE_COPY", "FeeStructure",
                String.valueOf(id), "Copied fee structure to " + created.size() + " class(es)");
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

    private boolean isYearEditable(AcademicYearStatus status) {
        return status == AcademicYearStatus.DRAFT
                || status == AcademicYearStatus.PREPARING
                || status == AcademicYearStatus.READY_FOR_APPROVAL
                || status == AcademicYearStatus.REJECTED
                || status == AcademicYearStatus.APPROVED;
    }

    private void assertYearEditable(Long yearId) {
        AcademicYear year = academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        if (!isYearEditable(year.getStatus())) {
            throw new BusinessException(YEAR_LOCKED_MESSAGE);
        }
    }

    private String resolveName(String requested, Long yearId, Long classId) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        String className = classRepository.findById(classId)
                .map(AcademicClass::getName)
                .orElse("Class");
        String yearName = academicYearRepository.findById(yearId)
                .map(AcademicYear::getName)
                .orElse("Year");
        String name = className + " — " + yearName;
        return name.length() > 150 ? name.substring(0, 150) : name;
    }

    private BigDecimal annualize(BigDecimal amount, FeeFrequency frequency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return switch (frequency) {
            case MONTHLY -> amount.multiply(BigDecimal.valueOf(12));
            case QUARTERLY -> amount.multiply(BigDecimal.valueOf(4));
            case HALF_YEARLY -> amount.multiply(BigDecimal.valueOf(2));
            case YEARLY, ONE_TIME -> amount;
        };
    }

    private FeeTotals computeTotals(List<FeeStructureItem> items) {
        BigDecimal mandatory = BigDecimal.ZERO;
        BigDecimal optional = BigDecimal.ZERO;
        BigDecimal monthlyRaw = BigDecimal.ZERO;
        boolean hasMonthly = false;
        for (FeeStructureItem item : items) {
            BigDecimal annualized = annualize(item.getAmount(), item.getFrequency());
            if (item.getType() == FeeItemType.MANDATORY) {
                mandatory = mandatory.add(annualized);
            } else {
                optional = optional.add(annualized);
            }
            if (item.getFrequency() == FeeFrequency.MONTHLY) {
                hasMonthly = true;
                monthlyRaw = monthlyRaw.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            }
        }
        return new FeeTotals(
                mandatory,
                optional,
                mandatory.add(optional),
                hasMonthly ? monthlyRaw : null,
                items.size());
    }

    private FeeStructureResponse toResponse(FeeStructure s) {
        List<FeeStructureItem> items =
                itemRepository.findByFeeStructureIdOrderByFeeStructureItemIdAsc(s.getFeeStructureId());
        FeeTotals totals = computeTotals(items);

        List<FeeStructureItemResponse> itemResponses = new ArrayList<>();
        for (FeeStructureItem item : items) {
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

        AcademicYear year = academicYearRepository.findById(s.getAcademicYearId()).orElse(null);
        AcademicClass academicClass = classRepository.findById(s.getClassId()).orElse(null);

        return FeeStructureResponse.builder()
                .feeStructureId(s.getFeeStructureId())
                .name(s.getName())
                .academicYearId(s.getAcademicYearId())
                .academicYearName(year != null ? year.getName() : null)
                .academicYearStatus(year != null ? year.getStatus() : null)
                .yearEditable(year != null && isYearEditable(year.getStatus()))
                .classId(s.getClassId())
                .className(academicClass != null ? academicClass.getName() : null)
                .classCode(academicClass != null ? academicClass.getCode() : null)
                .dueDay(s.getDueDay())
                .status(s.getStatus())
                .mandatorySum(totals.mandatorySum())
                .optionalSum(totals.optionalSum())
                .totalFees(totals.totalFees())
                .configuredMonthlyAmount(totals.configuredMonthlyAmount())
                .feeHeadCount(totals.feeHeadCount())
                .updatedOn(s.getUpdatedOn())
                .updatedBy(s.getUpdatedBy())
                .items(itemResponses)
                .build();
    }

    private record FeeTotals(
            BigDecimal mandatorySum,
            BigDecimal optionalSum,
            BigDecimal totalFees,
            BigDecimal configuredMonthlyAmount,
            int feeHeadCount) {
    }
}
