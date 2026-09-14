package com.thinkerscave.finance.service.impl;

import com.thinkerscave.finance.dto.response.OutstandingItemResponse;
import com.thinkerscave.finance.dto.response.OutstandingSummaryResponse;
import com.thinkerscave.finance.entity.StudentBillingPeriod;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.finance.repository.StudentBillingPeriodRepository;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.security.FinanceDataScopeResolver;
import com.thinkerscave.finance.service.FeeOutstandingService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeOutstandingServiceImpl implements FeeOutstandingService {

    private final FinanceAccessGuard accessGuard;
    private final FinanceDataScopeResolver scopeResolver;
    private final StudentBillingPeriodRepository billingPeriodRepository;
    private final StudentRepository studentRepository;

    @Override
    public PageResponse<OutstandingItemResponse> list(Long academicYearId, Long classId, Long sectionId,
                                                      BillingPeriodStatus status, Pageable pageable) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_OUTSTANDING);
        var scope = scopeResolver.resolve();
        boolean canCollect = accessGuard.canManage(FinanceAccessGuard.RESOURCE_COLLECTION);
        List<OutstandingItemResponse> items = new ArrayList<>();
        for (Long studentId : scope.accessibleStudentIds()) {
            List<StudentBillingPeriod> periods = billingPeriodRepository
                    .findByStudentIdAndAcademicYearIdOrderByDueDateAscPeriodStartAscStudentBillingPeriodIdAsc(
                            studentId, academicYearId);
            Student student = studentRepository.findById(studentId).orElse(null);
            for (StudentBillingPeriod p : periods) {
                if (p.getBalanceAmount() == null || p.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
                if (status != null && p.getStatus() != status) continue;
                if (classId != null && !classId.equals(p.getClassId())) continue;
                if (sectionId != null && !sectionId.equals(p.getSectionId())) continue;
                items.add(OutstandingItemResponse.builder()
                        .studentId(studentId)
                        .studentName(student != null ? (student.getFirstName() + " " + student.getLastName()).trim() : null)
                        .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                        .className(p.getClassName())
                        .sectionName(p.getSectionName())
                        .studentBillingPeriodId(p.getStudentBillingPeriodId())
                        .periodKey(p.getPeriodKey())
                        .periodLabel(p.getPeriodLabel())
                        .dueDate(p.getDueDate())
                        .balanceAmount(p.getBalanceAmount())
                        .status(p.getStatus())
                        .canCollectFee(canCollect)
                        .build());
            }
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<OutstandingItemResponse> page = start >= items.size() ? List.of() : items.subList(start, end);
        return PageResponse.of(new PageImpl<>(page, pageable, items.size()));
    }

    @Override
    public OutstandingSummaryResponse summary(Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_OUTSTANDING);
        var scope = scopeResolver.resolve();
        List<Long> ids = scope.organizationWide() ? null : new ArrayList<>(scope.accessibleStudentIds());
        BigDecimal total = billingPeriodRepository.sumOutstandingForYear(academicYearId, ids);
        return OutstandingSummaryResponse.builder()
                .totalOutstanding(total == null ? BigDecimal.ZERO : total)
                .overdueCount(0)
                .dueCount(0)
                .partiallyPaidCount(0)
                .build();
    }
}
