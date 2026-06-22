package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class TeacherWorkloadResponse {
    private Long teacherId;
    private Long academicYearId;
    private int totalPeriodsPerWeek;
    private List<SubjectAllocationItem> allocations;

    @Getter
    @Setter
    @Builder
    public static class SubjectAllocationItem {
        private Long assignmentId;
        private String subjectName;
        private String className;
        private String sectionName;
        private Integer periodsPerWeek;
    }
}
