package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.dto.request.AttendanceReportRequest;
import com.thinkerscave.attendance.dto.response.AttendanceSummaryReportResponse;
import com.thinkerscave.attendance.dto.response.StaffAttendanceSummaryResponse;
import com.thinkerscave.attendance.dto.response.StudentHistoryResponse;

import java.util.List;

public interface AttendanceReportService {

    /**
     * Get class-wise attendance summary for a date range.
     */
    AttendanceSummaryReportResponse getSummaryReport(AttendanceReportRequest request);

    /**
     * Get staff attendance summary for a date range.
     */
    List<StaffAttendanceSummaryResponse> getStaffReport(AttendanceReportRequest request);

    /**
     * Get detailed attendance report for an individual student.
     */
    StudentHistoryResponse getStudentReport(Long studentId, AttendanceReportRequest request);
}
