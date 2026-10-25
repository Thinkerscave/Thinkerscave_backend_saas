package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
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
public class SubjectUsageResponse {
    private Long mappingId;
    private Long academicGroupId;
    private String academicGroupName;
    private String academicGroupCode;
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private SubjectRequirement requirement;
    private Short weeklyPeriods;
    private SubjectTimetablePreference timetablePreference;
    private SubjectMappingScope scope;
    private int sectionCount;
    private int totalSectionCount;
    private boolean active;
}
