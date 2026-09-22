package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.dto.request.MarkStaffAttendanceRequest;
import com.thinkerscave.attendance.dto.request.StaffSignInRequest;
import com.thinkerscave.attendance.dto.request.StaffSignOutRequest;
import com.thinkerscave.attendance.dto.response.StaffAttendanceHistoryDayResponse;
import com.thinkerscave.attendance.dto.response.StaffAttendanceResponse;
import com.thinkerscave.attendance.dto.response.StaffAttendanceTodayResponse;
import com.thinkerscave.staff.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface StaffAttendanceService {

    StaffAttendanceResponse markAttendance(MarkStaffAttendanceRequest request);

    StaffAttendanceTodayResponse signIn(StaffSignInRequest request);

    StaffAttendanceTodayResponse signOut(StaffSignOutRequest request);

    List<StaffAttendanceResponse> getTodayAttendance(LocalDate date);

    Page<StaffAttendanceResponse> getStaffHistory(Long staffId, Pageable pageable);

    List<StaffAttendanceResponse> getStaffAttendanceByRange(Long staffId, LocalDate from, LocalDate to);

    StaffAttendanceTodayResponse getMyTodayStatus(String username);

    StaffAttendanceTodayResponse buildTodayStatus(Staff staff);

    /**
     * Full calendar/list history for the authenticated staff member across a date range,
     * including weekends, holidays, leave, and absent days with no record.
     */
    List<StaffAttendanceHistoryDayResponse> getMyHistory(LocalDate from, LocalDate to);

    int autoCloseExpiredSessions();
}
