package com.thinkerscave.academics.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record TeacherArrangementResponse(
        Long arrangementId,
        Long academicYearId,
        String academicYearName,
        LocalDate absenceDate,
        Long absentTeacherId,
        String absentTeacherName,
        String reason,
        String status,
        Long reviewedByUserId,
        LocalDateTime reviewedAt,
        String reviewNote,
        List<Slot> slots) {

    public record Slot(
            Long timetableEntryId,
            String academicGroupName,
            String sectionName,
            String subjectName,
            Short periodNumber,
            LocalTime startTime,
            LocalTime endTime,
            Long substituteStaffId,
            String substituteTeacherName) {}
}