package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
import com.thinkerscave.finance.enums.FeeReceiptStatus;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface FeeReceiptService {
    PageResponse<FeeReceiptResponse> list(
            String q,
            Long academicYearId,
            Long studentId,
            Long classId,
            Long sectionId,
            String paymentMethod,
            FeeReceiptStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable);

    FeeReceiptResponse get(Long id);

    FeeReceiptResponse preview(Long id);

    byte[] pdf(Long id);
}
