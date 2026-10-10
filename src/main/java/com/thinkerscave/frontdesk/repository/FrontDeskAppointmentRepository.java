package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskAppointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrontDeskAppointmentRepository extends JpaRepository<FrontDeskAppointment, Long> {

    @Query("""
            SELECT a FROM FrontDeskAppointment a
            WHERE a.organizationId = :orgId
              AND (:keyword IS NULL OR LOWER(a.visitorName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(COALESCE(a.staffName,'')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<FrontDeskAppointment> search(@Param("orgId") Long orgId, @Param("keyword") String keyword, Pageable pageable);

    long countByOrganizationIdAndRequestedSlotBetween(Long organizationId, java.time.LocalDateTime from, java.time.LocalDateTime to);
}

