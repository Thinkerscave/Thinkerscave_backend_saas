package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskVisitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrontDeskVisitorRepository extends JpaRepository<FrontDeskVisitor, Long> {

    @Query("""
            SELECT v FROM FrontDeskVisitor v
            WHERE v.organizationId = :orgId
              AND (:keyword IS NULL OR LOWER(v.visitorName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(v.mobileNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(COALESCE(v.badgeNumber,'')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<FrontDeskVisitor> search(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT v FROM FrontDeskVisitor v
            WHERE v.organizationId = :orgId
              AND v.outTime IS NULL
              AND (:keyword IS NULL OR LOWER(v.visitorName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(v.mobileNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<FrontDeskVisitor> searchActive(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);

    long countByOrganizationIdAndOutTimeIsNull(Long organizationId);
}
