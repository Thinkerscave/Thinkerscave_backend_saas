package com.thinkerscave.finance.service.impl;

import com.thinkerscave.finance.dto.response.DashboardKpiResponse;
import com.thinkerscave.finance.dto.response.FeePaymentResponse;
import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
import com.thinkerscave.finance.dto.response.OutstandingItemResponse;
import com.thinkerscave.finance.entity.FeePayment;
import com.thinkerscave.finance.entity.FeeReceipt;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.finance.repository.FeePaymentRepository;
import com.thinkerscave.finance.repository.FeeReceiptRepository;
import com.thinkerscave.finance.repository.StudentBillingPeriodRepository;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.security.FinanceDataScopeResolver;
import com.thinkerscave.finance.service.FeeDashboardService;
import com.thinkerscave.finance.service.FeeOutstandingService;
import com.thinkerscave.finance.service.FeeReceiptService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeDashboardServiceImpl implements FeeDashboardService {

    private final FinanceAccessGuard accessGuard;
    private final FinanceDataScopeResolver scopeResolver;
    private final StudentBillingPeriodRepository billingPeriodRepository;
    private final FeePaymentRepository paymentRepository;
    private final FeeReceiptRepository receiptRepository;
    private final FeeOutstandingService outstandingService;
    private final FeeReceiptService feeReceiptService;
    private final StudentRepository studentRepository;

    @Override
    public DashboardKpiResponse kpis(Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_MANAGEMENT);
        var scope = scopeResolver.resolve();
        List<Long> ids = scope.organizationWide() ? null : new ArrayList<>(scope.accessibleStudentIds());
        BigDecimal generated = nz(billingPeriodRepository.sumTotalForYear(academicYearId, ids));
        BigDecimal collected = nz(paymentRepository.sumPaymentsForYear(academicYearId, ids));
        BigDecimal outstanding = nz(billingPeriodRepository.sumOutstandingForYear(academicYearId, ids));
        BigDecimal overdue = nz(billingPeriodRepository.sumByStatus(academicYearId, BillingPeriodStatus.OVERDUE, ids));
        return DashboardKpiResponse.builder()
                .generated(generated)
                .collected(collected)
                .outstanding(outstanding)
                .overdue(overdue)
                .canCollectFee(accessGuard.canManage(FinanceAccessGuard.RESOURCE_COLLECTION))
                .build();
    }

    @Override
    public List<Map<String, Object>> collectionTrend(Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_MANAGEMENT);
        var scope = scopeResolver.resolve();
        List<Long> ids = scope.organizationWide() ? null : new ArrayList<>(scope.accessibleStudentIds());

        Map<String, BigDecimal> collectedByMonth = new HashMap<>();
        for (Object[] row : paymentRepository.sumCollectedByMonth(academicYearId, ids)) {
            String key = monthKey(row[0], row[1]);
            collectedByMonth.put(key, toBigDecimal(row[2]));
        }
        Map<String, BigDecimal> dueByMonth = new HashMap<>();
        for (Object[] row : billingPeriodRepository.sumDueByMonth(academicYearId, ids)) {
            String key = monthKey(row[0], row[1]);
            dueByMonth.put(key, toBigDecimal(row[2]));
        }

        java.util.TreeSet<String> months = new java.util.TreeSet<>();
        months.addAll(collectedByMonth.keySet());
        months.addAll(dueByMonth.keySet());

        List<Map<String, Object>> out = new ArrayList<>();
        for (String month : months) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", month);
            point.put("collected", collectedByMonth.getOrDefault(month, BigDecimal.ZERO));
            point.put("due", dueByMonth.getOrDefault(month, BigDecimal.ZERO));
            out.add(point);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> paymentStatus(Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_MANAGEMENT);
        DashboardKpiResponse k = kpis(academicYearId);
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(Map.of("status", "COLLECTED", "amount", k.getCollected()));
        out.add(Map.of("status", "OUTSTANDING", "amount", k.getOutstanding()));
        out.add(Map.of("status", "OVERDUE", "amount", k.getOverdue()));
        return out;
    }

    @Override
    public List<OutstandingItemResponse> upcomingDues(Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_MANAGEMENT);
        return outstandingService.list(academicYearId, null, null, BillingPeriodStatus.DUE, Pageable.ofSize(10)).getContent();
    }

    @Override
    public PageResponse<FeePaymentResponse> recentCollections(Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_MANAGEMENT);
        var scope = scopeResolver.resolve();
        var page = paymentRepository.findAll(pageable);
        List<FeePaymentResponse> content = page.getContent().stream()
                .filter(p -> scope.organizationWide() || scope.accessibleStudentIds().contains(p.getStudentId()))
                .map(this::toPaymentResponse)
                .toList();
        return PageResponse.<FeePaymentResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(content.size())
                .totalPages(1)
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private FeePaymentResponse toPaymentResponse(FeePayment payment) {
        Student student = studentRepository.findById(payment.getStudentId()).orElse(null);
        FeeReceipt receipt = receiptRepository.findByFeePaymentId(payment.getFeePaymentId()).orElse(null);
        String studentName = student == null ? null
                : ((student.getFirstName() == null ? "" : student.getFirstName()) + " "
                + (student.getLastName() == null ? "" : student.getLastName())).trim();
        return FeePaymentResponse.builder()
                .feePaymentId(payment.getFeePaymentId())
                .studentId(payment.getStudentId())
                .studentName(studentName)
                .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                .academicYearId(payment.getAcademicYearId())
                .paymentMethodName(payment.getPaymentMethodName())
                .amount(payment.getAmount())
                .paidOn(payment.getPaidOn())
                .status(payment.getStatus())
                .receiptId(receipt != null ? receipt.getFeeReceiptId() : null)
                .receiptNumber(receipt != null ? receipt.getReceiptNumber() : null)
                .build();
    }

    @Override
    public PageResponse<OutstandingItemResponse> overdueStudents(Long academicYearId, Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_MANAGEMENT);
        return outstandingService.list(academicYearId, null, null, BillingPeriodStatus.OVERDUE, pageable);
    }

    @Override
    public PageResponse<OutstandingItemResponse> upcomingPeriodDues(Long academicYearId, Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_MANAGEMENT);
        return outstandingService.list(academicYearId, null, null, BillingPeriodStatus.DUE, pageable);
    }

    @Override
    public PageResponse<FeeReceiptResponse> recentReceipts(Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_MANAGEMENT);
        return feeReceiptService.list(null, null, pageable);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    private static String monthKey(Object year, Object month) {
        int y = year instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(year));
        int m = month instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(month));
        return String.format("%04d-%02d", y, m);
    }
}
