package com.thinkerscave.academics.dto.response;

import java.time.LocalTime;
import java.util.List;

public record TeacherArrangementEligibleSlotResponse(
        Long timetableEntryId,
        String className,
        String sectionName,
        String subjectName,
        Short periodNumber,
        LocalTime startTime,
        LocalTime endTime,
        List<StaffOption> candidates) {

    public record StaffOption(Long staffId, String name) {}
}