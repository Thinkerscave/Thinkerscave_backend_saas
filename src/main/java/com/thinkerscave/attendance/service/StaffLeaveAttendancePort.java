package com.thinkerscave.attendance.service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Integration port for Leave Management → Staff Attendance.
 * Returns approved leave covering a date when the Leave module is available.
 */
public interface StaffLeaveAttendancePort {

    Optional<ApprovedLeaveInfo> findApprovedLeave(Long organizationId, Long staffId, LocalDate date);

    record ApprovedLeaveInfo(String leaveLabel, String leaveType) {}
}
