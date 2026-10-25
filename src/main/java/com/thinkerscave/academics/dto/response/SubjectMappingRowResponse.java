package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.SubjectMappingScope;
import com.thinkerscave.academics.enums.SubjectRequirement;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.SubjectType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SubjectMappingRowResponse {
    private Long mappingId;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private SubjectType subjectType;
    private boolean subjectActive;
    private SubjectRequirement requirement;
    private Short weeklyPeriods;
    private SubjectTimetablePreference timetablePreference;
    private SubjectMappingScope scope;
    private List<Long> sectionIds;
    private List<String> sectionNames;
    private int totalSectionCount;
    private long teacherAllocationCount;
    private long timetableEntryCount;
}
