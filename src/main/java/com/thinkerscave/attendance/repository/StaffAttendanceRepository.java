package com.thinkerscave.attendance.repository;

import com.thinkerscave.attendance.entity.StaffAttendance;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {

    Optional<StaffAttendance> findByOrganizationIdAndStaffIdAndAttendanceDate(
            Long orgId, Long staffId, LocalDate date);

    List<StaffAttendance> findByOrganizationIdAndAttendanceDateOrderByStaffName(
            Long orgId, LocalDate date);

    List<StaffAttendance> findByOrganizationIdAndAttendanceDateAndStatusOrderByStaffName(
            Long orgId, LocalDate date, StaffAttendanceStatus status);

    Page<StaffAttendance> findByOrganizationIdAndStaffIdOrderByAttendanceDateDesc(
            Long orgId, Long staffId, Pageable pageable);

    List<StaffAttendance> findByOrganizationIdAndStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long orgId, Long staffId, LocalDate from, LocalDate to);

    List<StaffAttendance> findByOrganizationIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long orgId, LocalDate from, LocalDate to);

    long countByOrganizationIdAndAttendanceDateAndStatus(
            Long orgId, LocalDate date, StaffAttendanceStatus status);

    @Query("""
            SELECT sa.status, COUNT(sa)
            FROM StaffAttendance sa
            WHERE sa.organizationId = :orgId
              AND sa.attendanceDate = :date
            GROUP BY sa.status
            """)
    List<Object[]> countByStatusForDate(@Param("orgId") Long orgId, @Param("date") LocalDate date);

    @Query("""
            SELECT sa.staffId, sa.staffName, sa.department,
                   COUNT(sa) as totalDays,
                   SUM(CASE WHEN sa.status = 'PRESENT' THEN 1 WHEN sa.status = 'LATE' THEN 1 ELSE 0 END) as presentDays,
                   SUM(CASE WHEN sa.status = 'ABSENT' THEN 1 ELSE 0 END) as absentDays,
                   SUM(CASE WHEN sa.status = 'LATE' THEN 1 ELSE 0 END) as lateDays
            FROM StaffAttendance sa
            WHERE sa.organizationId = :orgId
              AND sa.attendanceDate BETWEEN :from AND :to
            GROUP BY sa.staffId, sa.staffName, sa.department
            ORDER BY sa.staffName
            """)
    List<Object[]> getStaffAttendanceSummary(@Param("orgId") Long orgId,
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
