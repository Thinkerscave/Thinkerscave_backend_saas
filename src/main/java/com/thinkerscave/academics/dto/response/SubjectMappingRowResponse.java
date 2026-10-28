package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.SubjectRequirement;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.SubjectType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SubjectMappingRowResponse {
    private Long mappingId;
    private Long choiceMappingId;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private SubjectType subjectType;
    private boolean subjectActive;
    private SubjectRequirement requirement;
    private Short weeklyPeriods;
    private SubjectTimetablePreference timetablePreference;
    private String choiceGroup;
    private Short consecutivePeriods;
    private long teacherAllocationCount;
    private long timetableEntryCount;
}
