package com.thinkerscave.attendance.repository;

import com.thinkerscave.attendance.entity.AttendanceFreeze;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceFreezeRepository extends JpaRepository<AttendanceFreeze, Long> {

    List<AttendanceFreeze> findByOrganizationIdAndActiveTrueOrderByFreezeFromDateDesc(Long orgId);

    @Query("""
            SELECT COUNT(f) > 0 FROM AttendanceFreeze f
            WHERE f.organizationId = :orgId
              AND f.active = true
              AND :date BETWEEN f.freezeFromDate AND f.freezeToDate
            """)
    boolean isDateFrozen(@Param("orgId") Long orgId, @Param("date") LocalDate date);
}
