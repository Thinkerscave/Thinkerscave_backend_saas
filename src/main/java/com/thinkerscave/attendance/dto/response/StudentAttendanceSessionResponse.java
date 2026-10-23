package com.thinkerscave.attendance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * A recorded attendance session (one class/section on one date) used by the
 * attendance history list. Status is derived — COMPLETED when every enrolled
 * student has a record, otherwise PARTIAL.
 */
@Getter
@Builder
public class StudentAttendanceSessionResponse {

    private final LocalDate date;
    private final Long classId;
    private final String className;
    private final Long sectionId;
    private final String sectionName;

    /** Number of enrolled students expected for the class/section. */
    private final int expectedStudents;
    /** Number of students with a saved record for this session. */
    private final int totalMarked;
    private final int present;
    private final int absent;
    private final int late;
    private final int excused;

    /** 0..100 attendance percent (present + late over total marked). */
    private final double attendancePercent;

    private final boolean holiday;
    private final boolean weekend;
    private final String dayReason;

    /** COMPLETED | PARTIAL */
    private final String status;
}