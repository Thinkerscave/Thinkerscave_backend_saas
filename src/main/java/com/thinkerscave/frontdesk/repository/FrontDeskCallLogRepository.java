package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrontDeskCallLogRepository extends JpaRepository<FrontDeskCallLog, Long> {

    @Query("""
            SELECT c FROM FrontDeskCallLog c
            WHERE c.organizationId = :orgId
              AND (:keyword IS NULL OR LOWER(c.callerName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<FrontDeskCallLog> search(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);
}
