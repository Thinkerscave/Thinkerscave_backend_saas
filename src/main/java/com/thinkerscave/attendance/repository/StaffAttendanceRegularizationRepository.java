package com.thinkerscave.attendance.repository;

import com.thinkerscave.attendance.entity.StaffAttendanceRegularization;
import com.thinkerscave.attendance.enums.RegularizationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAttendanceRegularizationRepository extends JpaRepository<StaffAttendanceRegularization, Long> {

    List<StaffAttendanceRegularization> findByOrganizationIdAndStaffIdOrderByRequestedAtDesc(
            Long organizationId, Long staffId);

    List<StaffAttendanceRegularization> findByOrganizationIdAndStaffIdAndStatusOrderByRequestedAtDesc(
            Long organizationId, Long staffId, RegularizationRequestStatus status);

    List<StaffAttendanceRegularization> findByOrganizationIdAndStatusOrderByRequestedAtAsc(
            Long organizationId, RegularizationRequestStatus status);

    Optional<StaffAttendanceRegularization> findByOrganizationIdAndRequestId(Long organizationId, Long requestId);

    Optional<StaffAttendanceRegularization> findByOrganizationIdAndStaffIdAndAttendanceDateAndStatus(
            Long organizationId, Long staffId, LocalDate attendanceDate, RegularizationRequestStatus status);

    List<StaffAttendanceRegularization> findByOrganizationIdAndStaffIdAndAttendanceDateBetween(
            Long organizationId, Long staffId, LocalDate from, LocalDate to);

    long countByOrganizationIdAndStatus(Long organizationId, RegularizationRequestStatus status);

    boolean existsByOrganizationIdAndStaffIdAndAttendanceDateAndStatus(
            Long organizationId, Long staffId, LocalDate attendanceDate, RegularizationRequestStatus status);
}
