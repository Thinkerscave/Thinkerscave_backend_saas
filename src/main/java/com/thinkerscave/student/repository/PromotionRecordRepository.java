package com.thinkerscave.student.repository;

import com.thinkerscave.student.entity.PromotionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRecordRepository extends JpaRepository<PromotionRecord, Long> {

    List<PromotionRecord> findByBatch_BatchIdOrderByRecordIdAsc(Long batchId);

    void deleteByBatch_BatchId(Long batchId);

    long countByBatch_BatchId(Long batchId);
}
