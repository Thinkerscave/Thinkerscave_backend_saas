package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.dto.request.MarkStudentAttendanceRequest;
import com.thinkerscave.attendance.dto.request.MarkPeriodAttendanceRequest;
import com.thinkerscave.attendance.dto.request.UpdateStudentAttendanceRequest;
import com.thinkerscave.attendance.dto.response.ClassAttendanceSummaryResponse;
import com.thinkerscave.attendance.dto.response.StudentAttendanceResponse;
import com.thinkerscave.attendance.dto.response.StudentHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface StudentAttendanceService {

    /**
     * Mark daily attendance for all students in a class/section.
     */
    ClassAttendanceSummaryResponse markDailyAttendance(MarkStudentAttendanceRequest request);

    /**
     * Mark period-wise attendance for all students in a class/section.
     */
    ClassAttendanceSummaryResponse markPeriodAttendance(MarkPeriodAttendanceRequest request);

    /**
     * Copy attendance from previous working day for a class/section.
     */
    ClassAttendanceSummaryResponse copyFromPreviousDay(Long classId, Long sectionId, LocalDate targetDate);

    /**
     * Get existing daily attendance for a class/section on a date.
     */
    ClassAttendanceSummaryResponse getClassAttendance(Long classId, Long sectionId, LocalDate date);

    /**
     * Update a single student's attendance record.
     */
    StudentAttendanceResponse updateAttendance(Long attendanceId, UpdateStudentAttendanceRequest request);

    /**
     * Get paginated attendance history for a specific student.
     */
    Page<StudentAttendanceResponse> getStudentHistory(Long studentId, Pageable pageable);

    /**
     * Get attendance records for a student within a date range.
     */
    StudentHistoryResponse getStudentHistoryByRange(Long studentId, LocalDate from, LocalDate to);
}
