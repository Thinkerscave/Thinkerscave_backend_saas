package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.SubjectCategory;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AcademicGroupSubjectMappingResponse {
    private Long academicGroupSubjectMappingId;
    private Long academicGroupId;
    private String academicGroupName;
    private String academicGroupCode;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private SubjectCategory category;

    private boolean included;
    private Short weeklyPeriods;
    private Short defaultWeeklyPeriods;
    private boolean periodsOverridden;
    private SubjectTimetablePreference timetablePreference;
    private Boolean active;

    /** ASSIGNED | MISSING | NONE */
    private String teacherStatus;
    private long teacherAllocationCount;
}
