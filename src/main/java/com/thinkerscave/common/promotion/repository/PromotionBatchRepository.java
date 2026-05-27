package com.thinkerscave.common.promotion.repository;

import com.thinkerscave.common.promotion.domain.PromotionBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionBatchRepository
        extends JpaRepository<PromotionBatch, Long>, JpaSpecificationExecutor<PromotionBatch> {

    Page<PromotionBatch> findByOrganizationId(Long organizationId, Pageable pageable);
}
