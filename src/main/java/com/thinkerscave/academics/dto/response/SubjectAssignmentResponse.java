package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SubjectAssignmentResponse {
    private Long subjectAssignmentId;
    private Long academicYearId;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private Long subjectId;
    private String subjectName;
    private Long teacherId;
    private Integer periodsPerWeek;
    private Boolean active;
    private String remarks;
}
