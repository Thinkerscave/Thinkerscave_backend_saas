package com.thinkerscave.attendance.dto.response;

import com.thinkerscave.attendance.enums.AttendanceMode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Roster for marking student attendance — authoritative student list comes from
 * active enrollments, merged with any attendance already saved for the date.
 * A {@code null} {@code status} means the student is not marked yet.
 */
@Getter
@Builder
public class StudentRosterResponse {

    private final LocalDate attendanceDate;
    private final Long classId;
    private final String className;
    private final Long sectionId;
    private final String sectionName;
    private final Long academicYearId;

    /** Org attendance mode — DAILY or PERIOD_WISE. */
    private final AttendanceMode attendanceMode;

    /** Whether this date is an instructional day for the class. */
    private final boolean attendanceRequired;
    private final boolean holiday;
    private final boolean weekend;
    private final String holidayName;
    private final String dayReason;

    /** True when attendance is frozen for this date and cannot be modified. */
    private final boolean frozen;

    /** True when the current org policy and day state allow saving attendance for this date. */
    private final boolean editable;

    /** Why attendance cannot be saved for this date; null when editable. */
    private final String editabilityReason;

    private final List<StudentRosterEntryResponse> students;

    @Getter
    @Builder
    public static class StudentRosterEntryResponse {
        private final Long studentId;
        private final String studentName;
        private final String rollNumber;
        private final String admissionNumber;
        /** Document id of the student's PHOTO document; the client loads it via the authenticated download endpoint. */
        private final Long photoDocumentId;
        private final com.thinkerscave.attendance.enums.StudentAttendanceStatus status;
        private final String remarks;
    }
}