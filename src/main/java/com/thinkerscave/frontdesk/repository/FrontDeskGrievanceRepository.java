package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskGrievance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrontDeskGrievanceRepository extends JpaRepository<FrontDeskGrievance, Long> {

    @Query("""
            SELECT g FROM FrontDeskGrievance g
            WHERE g.organizationId = :orgId
              AND (:keyword IS NULL OR LOWER(COALESCE(g.complainantName,'')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(g.category) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<FrontDeskGrievance> search(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);
}
