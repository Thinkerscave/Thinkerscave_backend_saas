package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.response.DashboardKpiResponse;
import com.thinkerscave.finance.dto.response.FeePaymentResponse;
import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
import com.thinkerscave.finance.dto.response.OutstandingItemResponse;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface FeeDashboardService {
    DashboardKpiResponse kpis(Long academicYearId);
    List<Map<String, Object>> collectionTrend(Long academicYearId);
    List<Map<String, Object>> paymentStatus(Long academicYearId);
    List<OutstandingItemResponse> upcomingDues(Long academicYearId);
    PageResponse<FeePaymentResponse> recentCollections(Pageable pageable);
    PageResponse<OutstandingItemResponse> overdueStudents(Long academicYearId, Pageable pageable);
    PageResponse<OutstandingItemResponse> upcomingPeriodDues(Long academicYearId, Pageable pageable);
    PageResponse<FeeReceiptResponse> recentReceipts(Pageable pageable);
}
