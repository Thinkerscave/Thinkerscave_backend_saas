package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.dto.response.AttendanceCalendarResponse;

public interface AttendanceCalendarService {

    /**
     * Get month-view calendar attendance data for a class/section.
     */
    AttendanceCalendarResponse getCalendarData(Long classId, Long sectionId, int year, int month);
}
