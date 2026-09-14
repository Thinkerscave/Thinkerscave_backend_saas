package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeeHead;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeeHeadRepository extends JpaRepository<FeeHead, Long> {

    Optional<FeeHead> findByNameNormalized(String nameNormalized);

    boolean existsByNameNormalized(String nameNormalized);

    List<FeeHead> findByStatusOrderByNameAsc(FeeMasterStatus status);

    @Query("""
            SELECT h FROM FeeHead h
            WHERE (:q IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(COALESCE(h.description, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
              AND (:category IS NULL OR h.category = :category)
              AND (:status IS NULL OR h.status = :status)
            """)
    Page<FeeHead> search(@Param("q") String q,
                         @Param("category") com.thinkerscave.finance.enums.FeeHeadCategory category,
                         @Param("status") FeeMasterStatus status,
                         Pageable pageable);
}
