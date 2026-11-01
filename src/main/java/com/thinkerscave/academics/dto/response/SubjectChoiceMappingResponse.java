package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.SubjectType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SubjectChoiceMappingResponse {
    private Long academicGroupChoiceMappingId;
    private Long choiceGroupId;
    private String name;
    private SubjectType subjectType;
    private Short weeklyPeriods;
    private SubjectTimetablePreference timetablePreference;
    private boolean active;
    private List<SubjectMappingRowResponse> subjects;
}
