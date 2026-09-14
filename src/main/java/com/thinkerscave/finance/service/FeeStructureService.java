package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.request.CloneFeeStructureRequest;
import com.thinkerscave.finance.dto.request.FeeStructureRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.ClonePreviewResponse;
import com.thinkerscave.finance.dto.response.FeeStructureResponse;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FeeStructureService {
    PageResponse<FeeStructureResponse> list(String q, Long academicYearId, Long classId, FeeMasterStatus status, Pageable pageable);
    FeeStructureResponse get(Long id);
    FeeStructureResponse create(FeeStructureRequest request);
    FeeStructureResponse update(Long id, FeeStructureRequest request);
    FeeStructureResponse updateStatus(Long id, StatusUpdateRequest request);
    void delete(Long id);
    ClonePreviewResponse clonePreview(Long id, CloneFeeStructureRequest request);
    List<FeeStructureResponse> clone(Long id, CloneFeeStructureRequest request);
}
