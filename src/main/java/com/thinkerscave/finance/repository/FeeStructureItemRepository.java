package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeeStructureItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeeStructureItemRepository extends JpaRepository<FeeStructureItem, Long> {

    List<FeeStructureItem> findByFeeStructureIdOrderByFeeStructureItemIdAsc(Long feeStructureId);

    void deleteByFeeStructureId(Long feeStructureId);

    boolean existsByFeeHeadId(Long feeHeadId);

    @Query("""
            SELECT COUNT(i) > 0 FROM FeeStructureItem i, FeeStructure s
            WHERE i.feeStructureId = s.feeStructureId
              AND i.feeHeadId = :feeHeadId
              AND s.status = com.thinkerscave.finance.enums.FeeMasterStatus.ACTIVE
            """)
    boolean existsOnActiveStructure(@Param("feeHeadId") Long feeHeadId);
}
