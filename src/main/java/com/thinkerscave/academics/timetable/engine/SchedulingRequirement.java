package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SchedulingRequirement {

    private String requirementId;
    /** Stable scheduling-unit id (section id, or negative group id for section-less groups). */
    private Long unitId;
    /** Real section id, null for a group-level unit. */
    private Long sectionId;
    private String sectionName;
    private Long academicGroupId;
    private String academicGroupName;
    private Long academicGroupSubjectMappingId;
    private Long subjectId;
    private String subjectName;
    private int weeklyPeriods;
    private int periodIndex;
    private Long teacherAllocationId;
    private Long primaryStaffId;
    private String primaryStaffName;
    private Long defaultResourceId;
    private SubjectTimetablePreference preference;
    private boolean optional;
    private int consecutivePeriods = 1;
    private List<CandidateSlot> candidates;
    private int staticCandidateCount;
}
