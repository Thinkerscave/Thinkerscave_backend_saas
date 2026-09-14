package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface FeeReceiptService {
    PageResponse<FeeReceiptResponse> list(String q, Long academicYearId, Pageable pageable);
    FeeReceiptResponse get(Long id);
    FeeReceiptResponse preview(Long id);
    byte[] pdf(Long id);
}
