package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskGatePass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrontDeskGatePassRepository extends JpaRepository<FrontDeskGatePass, Long> {

    @Query("""
            SELECT g FROM FrontDeskGatePass g
            WHERE g.organizationId = :orgId
              AND (:keyword IS NULL OR LOWER(g.studentName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<FrontDeskGatePass> search(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);
}
