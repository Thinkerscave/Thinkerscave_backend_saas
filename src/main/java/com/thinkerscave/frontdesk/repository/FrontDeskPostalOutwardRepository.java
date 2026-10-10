package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskPostalOutward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrontDeskPostalOutwardRepository extends JpaRepository<FrontDeskPostalOutward, Long> {

    @Query("""
            SELECT p FROM FrontDeskPostalOutward p
            WHERE p.organizationId = :orgId
              AND (:keyword IS NULL OR LOWER(p.recipientName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(COALESCE(p.trackingNumber,'')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<FrontDeskPostalOutward> search(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);

    long countByOrganizationIdAndDispatchDate(Long organizationId, java.time.LocalDate date);
}

