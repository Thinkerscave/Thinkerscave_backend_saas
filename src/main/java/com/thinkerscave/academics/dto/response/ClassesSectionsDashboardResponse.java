package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ClassesSectionsDashboardResponse {
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearReadOnly;

    private long classCount;
    private long classesActive;
    private long sectionCount;
    private long sectionsActive;
    private long studentCount;

    private List<AcademicClassResponse> classes;
}
