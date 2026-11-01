package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskCertificateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrontDeskCertificateRequestRepository extends JpaRepository<FrontDeskCertificateRequest, Long> {

    @Query("""
            SELECT c FROM FrontDeskCertificateRequest c
            WHERE c.organizationId = :orgId
              AND (:keyword IS NULL OR LOWER(c.studentName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(COALESCE(c.rollNumber,'')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<FrontDeskCertificateRequest> search(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);
}
