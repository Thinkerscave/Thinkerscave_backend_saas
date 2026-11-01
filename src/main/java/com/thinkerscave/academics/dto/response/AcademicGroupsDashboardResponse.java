package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AcademicGroupsDashboardResponse {
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearReadOnly;

    private long academicGroupCount;
    private long academicGroupsActive;
    private long sectionCount;
    private long sectionsActive;
    private long studentCount;

    private List<AcademicGroupResponse> academicGroups;
}
