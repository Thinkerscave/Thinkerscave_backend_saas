package com.thinkerscave.common.promotion.repository;

import com.thinkerscave.common.promotion.domain.PromotionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRecordRepository extends JpaRepository<PromotionRecord, Long> {

    List<PromotionRecord> findByPromotionBatchId(Long promotionBatchId);

    long countByPromotionBatchId(Long promotionBatchId);
}
