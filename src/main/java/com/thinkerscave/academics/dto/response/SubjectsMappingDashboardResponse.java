package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SubjectsMappingDashboardResponse {
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearReadOnly;

    private long classCount;
    private long sectionCount;
    private long subjectCount;
    private long subjectsActive;
    private long unmappedSubjectCount;

    private List<SubjectResponse> subjects;
}
