package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.response.OutstandingItemResponse;
import com.thinkerscave.finance.dto.response.OutstandingSummaryResponse;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface FeeOutstandingService {
    PageResponse<OutstandingItemResponse> list(Long academicYearId, Long classId, Long sectionId,
                                               BillingPeriodStatus status, Pageable pageable);
    OutstandingSummaryResponse summary(Long academicYearId);
}
