package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskPostalInward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrontDeskPostalInwardRepository extends JpaRepository<FrontDeskPostalInward, Long> {

    @Query("""
            SELECT p FROM FrontDeskPostalInward p
            WHERE p.organizationId = :orgId
              AND (:keyword IS NULL OR LOWER(p.senderName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(COALESCE(p.trackingNumber,'')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<FrontDeskPostalInward> search(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);
}
