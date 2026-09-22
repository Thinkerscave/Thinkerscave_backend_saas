package com.thinkerscave.finance.service.impl;

import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.finance.dto.response.FeeReceiptLineResponse;
import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
import com.thinkerscave.finance.entity.FeeReceipt;
import com.thinkerscave.finance.enums.FeeReceiptStatus;
import com.thinkerscave.finance.repository.FeeReceiptLineRepository;
import com.thinkerscave.finance.repository.FeeReceiptRepository;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.security.FinanceDataScopeResolver;
import com.thinkerscave.finance.service.FeeReceiptPdfService;
import com.thinkerscave.finance.service.FeeReceiptService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeReceiptServiceImpl implements FeeReceiptService {

    private final FeeReceiptRepository receiptRepository;
    private final FeeReceiptLineRepository receiptLineRepository;
    private final FinanceAccessGuard accessGuard;
    private final FinanceDataScopeResolver scopeResolver;
    private final FeeReceiptPdfService feeReceiptPdfService;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;

    @Override
    public PageResponse<FeeReceiptResponse> list(
            String q,
            Long academicYearId,
            Long studentId,
            Long classId,
            Long sectionId,
            String paymentMethod,
            FeeReceiptStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_RECEIPTS);
        var scope = scopeResolver.resolve();
        if (!scope.canSearchStudents()) {
            throw new AccessDeniedException("Receipt workspace requires searchable staff scope");
        }
        List<Long> ids;
        if (studentId != null) {
            scopeResolver.assertStudentAccess(studentId);
            if (scope.organizationWide() || scope.accessibleStudentIds().contains(studentId)) {
                ids = List.of(studentId);
            } else {
                return PageResponse.of(org.springframework.data.domain.Page.empty(pageable), this::toResponse);
            }
        } else if (scope.organizationWide()) {
            ids = null;
        } else {
            ids = new ArrayList<>(scope.accessibleStudentIds());
            if (ids.isEmpty()) {
                return PageResponse.of(org.springframework.data.domain.Page.empty(pageable), this::toResponse);
            }
        }
        String qq = q == null || q.isBlank() ? null : q.trim();
        String method = paymentMethod == null || paymentMethod.isBlank() ? null : paymentMethod.trim();
        String className = classId == null ? null
                : classRepository.findById(classId).map(c -> c.getName()).orElse("__missing__");
        String sectionName = sectionId == null ? null
                : sectionRepository.findById(sectionId).map(s -> s.getName()).orElse("__missing__");
        boolean hasExtraFilters = method != null || className != null || sectionName != null || fromDate != null || toDate != null;
        if (!hasExtraFilters) {
            return PageResponse.of(receiptRepository.search(qq, academicYearId, ids, pageable), this::toResponse);
        }
        LocalDateTime fromOn = fromDate != null ? fromDate.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime toOn = toDate != null ? toDate.atTime(LocalTime.MAX) : LocalDateTime.of(2099, 12, 31, 23, 59, 59);
        return PageResponse.of(
                receiptRepository.searchFiltered(qq, academicYearId, ids, method, className, sectionName, fromOn, toOn, pageable),
                this::toResponse);
    }

    @Override
    public FeeReceiptResponse get(Long id) {
        FeeReceipt receipt = require(id);
        assertView(receipt);
        return toResponse(receipt);
    }

    @Override
    public FeeReceiptResponse preview(Long id) {
        return get(id);
    }

    @Override
    public byte[] pdf(Long id) {
        FeeReceiptResponse receipt = get(id);
        return feeReceiptPdfService.build(receipt);
    }

    private void assertView(FeeReceipt receipt) {
        boolean ok = accessGuard.canView(FinanceAccessGuard.RESOURCE_RECEIPTS)
                || accessGuard.canView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        if (!ok) {
            accessGuard.requireView(FinanceAccessGuard.RESOURCE_RECEIPTS);
        }
        scopeResolver.assertStudentAccess(receipt.getStudentId());
    }

    private FeeReceipt require(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));
    }

    private FeeReceiptResponse toResponse(FeeReceipt r) {
        var lines = receiptLineRepository.findByFeeReceiptIdOrderByFeeReceiptLineIdAsc(r.getFeeReceiptId()).stream()
                .map(l -> FeeReceiptLineResponse.builder()
                        .feeReceiptLineId(l.getFeeReceiptLineId())
                        .studentBillingPeriodId(l.getStudentBillingPeriodId())
                        .periodKey(l.getPeriodKey())
                        .periodLabel(l.getPeriodLabel())
                        .amount(l.getAmount())
                        .build())
                .toList();
        return FeeReceiptResponse.builder()
                .feeReceiptId(r.getFeeReceiptId())
                .feePaymentId(r.getFeePaymentId())
                .receiptNumber(r.getReceiptNumber())
                .issuedOn(r.getIssuedOn())
                .status(r.getStatus())
                .studentId(r.getStudentId())
                .studentName(r.getStudentName())
                .admissionNumber(r.getAdmissionNumber())
                .className(r.getClassName())
                .sectionName(r.getSectionName())
                .academicYearId(r.getAcademicYearId())
                .academicYearName(r.getAcademicYearName())
                .amount(r.getAmount())
                .paymentMethodName(r.getPaymentMethodName())
                .referenceNumber(r.getReferenceNumber())
                .remarks(r.getRemarks())
                .schoolName(r.getSchoolName())
                .schoolLogoUrl(r.getSchoolLogoUrl())
                .schoolAddress(r.getSchoolAddress())
                .schoolContact(r.getSchoolContact())
                .currencyCode(r.getCurrencyCode())
                .lines(lines)
                .build();
    }
}
