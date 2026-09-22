package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.request.CloneFeeStructureRequest;
import com.thinkerscave.finance.dto.request.ConfigureClassFeeStructureRequest;
import com.thinkerscave.finance.dto.request.CopyClassFeeStructureRequest;
import com.thinkerscave.finance.dto.request.FeeStructureRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.ClassFeeStructureOverviewResponse;
import com.thinkerscave.finance.dto.response.ClonePreviewResponse;
import com.thinkerscave.finance.dto.response.FeeStructureResponse;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FeeStructureService {
    PageResponse<FeeStructureResponse> list(String q, Long academicYearId, Long classId, FeeMasterStatus status, Pageable pageable);

    /**
     * Class-wise overview for an academic year: every active class as Configured or Not Configured.
     * @param configuredFilter ALL | CONFIGURED | NOT_CONFIGURED
     */
    PageResponse<ClassFeeStructureOverviewResponse> classOverview(
            Long academicYearId, String q, String configuredFilter, Pageable pageable);

    FeeStructureResponse get(Long id);

    FeeStructureResponse getByClass(Long academicYearId, Long classId);

    FeeStructureResponse create(FeeStructureRequest request);

    FeeStructureResponse update(Long id, FeeStructureRequest request);

    /** Upsert ACTIVE structure for class + year (no structure name from client). */
    FeeStructureResponse configure(ConfigureClassFeeStructureRequest request);

    FeeStructureResponse updateStatus(Long id, StatusUpdateRequest request);

    void delete(Long id);

    ClonePreviewResponse clonePreview(Long id, CloneFeeStructureRequest request);

    List<FeeStructureResponse> clone(Long id, CloneFeeStructureRequest request);

    /** Copy structure to other classes in the same academic year. */
    List<FeeStructureResponse> copyToClasses(Long id, CopyClassFeeStructureRequest request);
}
