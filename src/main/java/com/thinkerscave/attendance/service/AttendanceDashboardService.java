package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.dto.response.AttendanceDashboardResponse;

import java.time.LocalDate;

public interface AttendanceDashboardService {

    /**
     * Get attendance dashboard statistics for the current organization on the given date.
     */
    AttendanceDashboardResponse getDashboardStats(LocalDate date);
}
