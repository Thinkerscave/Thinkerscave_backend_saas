package com.thinkerscave.student.repository;

import com.thinkerscave.student.entity.PromotionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionBatchRepository extends JpaRepository<PromotionBatch, Long> {
}
