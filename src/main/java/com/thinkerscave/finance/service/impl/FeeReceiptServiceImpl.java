package com.thinkerscave.finance.service.impl;

import com.thinkerscave.finance.dto.response.FeeReceiptLineResponse;
import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
import com.thinkerscave.finance.entity.FeeReceipt;
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

    @Override
    public PageResponse<FeeReceiptResponse> list(String q, Long academicYearId, Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_RECEIPTS);
        var scope = scopeResolver.resolve();
        if (!scope.canSearchStudents()) {
            throw new AccessDeniedException("Receipt workspace requires searchable staff scope");
        }
        List<Long> ids = scope.organizationWide() ? null : new ArrayList<>(scope.accessibleStudentIds());
        String qq = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(receiptRepository.search(qq, academicYearId, ids, pageable), this::toResponse);
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
