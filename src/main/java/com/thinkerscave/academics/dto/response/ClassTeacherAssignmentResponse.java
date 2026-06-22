package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ClassTeacherAssignmentResponse {
    private Long assignmentId;
    private Long academicYearId;
    private String yearCode;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private Long teacherId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private LocalDateTime createdOn;
}
