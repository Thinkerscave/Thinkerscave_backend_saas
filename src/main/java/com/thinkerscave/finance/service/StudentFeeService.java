package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.response.*;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentFeeService {
    PageResponse<StudentFeeListItemResponse> list(
            String q,
            Long academicYearId,
            Long classId,
            Long sectionId,
            BillingPeriodStatus status,
            String periodKey,
            Boolean outstandingOnly,
            Pageable pageable);

    StudentFeeSummaryResponse summary(
            String q,
            Long academicYearId,
            Long classId,
            Long sectionId,
            BillingPeriodStatus status,
            String periodKey,
            Boolean outstandingOnly);

    List<BillingPeriodOptionResponse> billingPeriodOptions(Long academicYearId);

    List<StudentFeeListItemResponse> search(String q, Long academicYearId);

    StudentFeeDetailResponse detail(Long studentId, Long academicYearId);

    List<BillingPeriodResponse> periods(Long studentId, Long academicYearId);

    List<FeePaymentResponse> payments(Long studentId, Long academicYearId);

    List<FeeReceiptResponse> receipts(Long studentId, Long academicYearId);

    List<AcademicYearOptionResponse> academicYears(Long studentId);

    StudentFeeKpiResponse outstanding(Long studentId, Long academicYearId);

    List<LinkedStudentResponse> linked();
}
