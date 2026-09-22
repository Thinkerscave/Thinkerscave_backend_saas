package com.thinkerscave.finance.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.dto.request.FeeHeadRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.FeeHeadResponse;
import com.thinkerscave.finance.entity.FeeHead;
import com.thinkerscave.finance.enums.FeeHeadCategory;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.repository.FeeHeadRepository;
import com.thinkerscave.finance.repository.FeeStructureItemRepository;
import com.thinkerscave.finance.repository.StudentBillingPeriodLineRepository;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.service.FeeHeadService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeHeadServiceImpl implements FeeHeadService {

    private final FeeHeadRepository feeHeadRepository;
    private final FeeStructureItemRepository structureItemRepository;
    private final StudentBillingPeriodLineRepository billingPeriodLineRepository;
    private final FinanceAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    public PageResponse<FeeHeadResponse> list(String q, FeeHeadCategory category, FeeMasterStatus status, Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_HEADS);
        String qq = blankToNull(q);
        return PageResponse.of(feeHeadRepository.search(qq, category, status, pageable), this::toResponse);
    }

    @Override
    public FeeHeadResponse get(Long id) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_HEADS);
        return toResponse(require(id));
    }

    @Override
    public List<FeeHeadResponse> lookups() {
        if (!accessGuard.canView(FinanceAccessGuard.RESOURCE_HEADS)
                && !accessGuard.canView(FinanceAccessGuard.RESOURCE_STRUCTURES)) {
            accessGuard.requireView(FinanceAccessGuard.RESOURCE_HEADS);
        }
        return feeHeadRepository.findByStatusOrderByNameAsc(FeeMasterStatus.ACTIVE).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public FeeHeadResponse create(FeeHeadRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_HEADS);
        String normalized = normalize(request.getName());
        if (feeHeadRepository.existsByNameNormalized(normalized)) {
            throw new AlreadyExistsException("Fee head name already exists", "name");
        }
        FeeHead head = new FeeHead();
        apply(head, request, normalized);
        if (head.getStatus() == null) {
            head.setStatus(FeeMasterStatus.ACTIVE);
        }
        FeeHead saved = feeHeadRepository.save(head);
        auditWriteService.record(AuditEventType.CREATE, "FEE_HEAD_CREATE", "FeeHead",
                String.valueOf(saved.getFeeHeadId()), "Created fee head " + saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FeeHeadResponse update(Long id, FeeHeadRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_HEADS);
        FeeHead head = require(id);
        String normalized = normalize(request.getName());
        feeHeadRepository.findByNameNormalized(normalized).ifPresent(existing -> {
            if (!existing.getFeeHeadId().equals(id)) {
                throw new AlreadyExistsException("Fee head name already exists", "name");
            }
        });
        apply(head, request, normalized);
        FeeHead saved = feeHeadRepository.save(head);
        auditWriteService.record(AuditEventType.UPDATE, "FEE_HEAD_UPDATE", "FeeHead",
                String.valueOf(id), "Updated fee head");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FeeHeadResponse updateStatus(Long id, StatusUpdateRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_HEADS);
        FeeHead head = require(id);
        if (request.getStatus() == FeeMasterStatus.INACTIVE
                && structureItemRepository.existsOnActiveStructure(id)) {
            throw new BusinessException("Cannot deactivate fee head while used by an ACTIVE fee structure");
        }
        head.setStatus(request.getStatus());
        FeeHead saved = feeHeadRepository.save(head);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "FEE_HEAD_STATUS", "FeeHead",
                String.valueOf(id), "Status -> " + request.getStatus());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_HEADS);
        FeeHead head = require(id);
        if (structureItemRepository.existsByFeeHeadId(id) || billingPeriodLineRepository.existsByFeeHeadId(id)) {
            throw new BusinessException("Cannot delete fee head referenced by structures or billing history; deactivate instead");
        }
        feeHeadRepository.delete(head);
        auditWriteService.record(AuditEventType.DELETE, "FEE_HEAD_DELETE", "FeeHead",
                String.valueOf(id), "Deleted fee head");
    }

    private FeeHead require(Long id) {
        return feeHeadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee head not found"));
    }

    private void apply(FeeHead head, FeeHeadRequest request, String normalized) {
        head.setName(request.getName().trim());
        head.setNameNormalized(normalized);
        head.setCategory(request.getCategory());
        head.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            head.setStatus(request.getStatus());
        }
    }

    private FeeHeadResponse toResponse(FeeHead h) {
        return FeeHeadResponse.builder()
                .feeHeadId(h.getFeeHeadId())
                .name(h.getName())
                .category(h.getCategory())
                .description(h.getDescription())
                .status(h.getStatus())
                .build();
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    private static String blankToNull(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }
}
