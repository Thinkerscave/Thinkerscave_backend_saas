package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SchedulingSourceData {

    private final TimetableConfiguration configuration;
    private final List<DayOfWeek> workingDays;
    private final List<TimetablePeriod> teachingPeriods;
    private final List<TimetablePeriod> allPeriods;
    private final List<AcademicSection> activeSections;
    private final List<ClassSubjectMapping> activeMappings;
    private final List<TeacherAllocationTeacher> activePrimaries;
    private final int maxTeacherWeeklyPeriods;
    private final int halfIndex;
}
