package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.dto.request.MarkStaffAttendanceRequest;
import com.thinkerscave.attendance.dto.request.StaffSignInRequest;
import com.thinkerscave.attendance.dto.request.StaffSignOutRequest;
import com.thinkerscave.attendance.dto.response.StaffAttendanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface StaffAttendanceService {

    /**
     * Mark or update attendance for a staff member.
     */
    StaffAttendanceResponse markAttendance(MarkStaffAttendanceRequest request);

    /**
     * Record staff sign-in time (current time).
     */
    StaffAttendanceResponse signIn(StaffSignInRequest request);

    /**
     * Record staff sign-out time (current time) and compute working minutes.
     */
    StaffAttendanceResponse signOut(StaffSignOutRequest request);

    /**
     * Get all staff attendance for an organization on a date.
     */
    List<StaffAttendanceResponse> getTodayAttendance(LocalDate date);

    /**
     * Get paginated attendance history for a specific staff member.
     */
    Page<StaffAttendanceResponse> getStaffHistory(Long staffId, Pageable pageable);

    /**
     * Get staff attendance within a date range.
     */
    List<StaffAttendanceResponse> getStaffAttendanceByRange(Long staffId, LocalDate from, LocalDate to);
}
