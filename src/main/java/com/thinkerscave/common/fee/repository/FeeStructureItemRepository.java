package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeStructureItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeStructureItemRepository extends JpaRepository<FeeStructureItem, Long> {
    List<FeeStructureItem> findByFeeStructureId(Long feeStructureId);
    void deleteByFeeStructureId(Long feeStructureId);
}
