package com.thinkerscave.student.service;

import com.thinkerscave.student.dto.request.PromotionBatchCreateRequest;
import com.thinkerscave.student.dto.request.PromotionRecordUpdateRequest;
import com.thinkerscave.student.dto.response.PromotionBatchResponse;
import com.thinkerscave.student.dto.response.PromotionRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PromotionBatchService {

    Page<PromotionBatchResponse> list(Pageable pageable);

    PromotionBatchResponse create(PromotionBatchCreateRequest request);

    List<PromotionRecordResponse> preview(Long batchId);

    List<PromotionRecordResponse> records(Long batchId);

    PromotionRecordResponse updateRecord(Long recordId, PromotionRecordUpdateRequest request);

    PromotionBatchResponse execute(Long batchId);

    PromotionBatchResponse rollback(Long batchId);

    PromotionBatchResponse cancel(Long batchId);
}
