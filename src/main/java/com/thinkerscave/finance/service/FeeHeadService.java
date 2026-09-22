package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.request.FeeHeadRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.FeeHeadResponse;
import com.thinkerscave.finance.enums.FeeHeadCategory;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FeeHeadService {
    PageResponse<FeeHeadResponse> list(String q, FeeHeadCategory category, FeeMasterStatus status, Pageable pageable);
    FeeHeadResponse get(Long id);
    List<FeeHeadResponse> lookups();
    FeeHeadResponse create(FeeHeadRequest request);
    FeeHeadResponse update(Long id, FeeHeadRequest request);
    FeeHeadResponse updateStatus(Long id, StatusUpdateRequest request);
    void delete(Long id);
}
