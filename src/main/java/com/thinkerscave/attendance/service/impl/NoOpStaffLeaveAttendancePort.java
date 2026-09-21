package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.service.StaffLeaveAttendancePort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Placeholder until Leave Management is wired. Always reports no approved leave
 * so attendance gating remains ready for a future Leave module implementation.
 */
@Component
public class NoOpStaffLeaveAttendancePort implements StaffLeaveAttendancePort {

    @Override
    public Optional<ApprovedLeaveInfo> findApprovedLeave(Long organizationId, Long staffId, LocalDate date) {
        return Optional.empty();
    }
}
