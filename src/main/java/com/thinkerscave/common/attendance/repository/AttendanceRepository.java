package com.thinkerscave.common.attendance.repository;

import com.thinkerscave.common.attendance.domain.Attendance;
import com.thinkerscave.common.attendance.domain.Attendance.AttendanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Queries run in the CURRENT TENANT SCHEMA (set by TenantFilter + Hibernate
 * schema routing).
 * All methods include organizationId to enforce branch-level data isolation
 * within a tenant.
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByOrganizationIdAndAttendanceDateAndAttendanceType(
            Long organizationId, LocalDate date, AttendanceType type);

    List<Attendance> findByOrganizationIdAndClassIdAndAttendanceDate(
            Long organizationId, Long classId, LocalDate date);

    List<Attendance> findByOrganizationIdAndReferenceIdAndAttendanceType(
            Long organizationId, Long referenceId, AttendanceType type);

    List<Attendance> findByOrganizationId(Long organizationId);
}
