package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SchedulingRequirement {

    private String requirementId;
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
    private List<CandidateSlot> candidates;
    private int staticCandidateCount;
}
