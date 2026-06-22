package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectAssignmentRequest {

    @NotNull(message = "Academic year ID is mandatory")
    private Long academicYearId;

    @NotNull(message = "Class ID is mandatory")
    private Long classId;

    private Long sectionId;

    @NotNull(message = "Subject ID is mandatory")
    private Long subjectId;

    @NotNull(message = "Teacher ID is mandatory")
    private Long teacherId;

    @NotNull(message = "Periods per week is mandatory")
    @Min(value = 1, message = "Periods per week must be at least 1")
    private Integer periodsPerWeek;

    private String remarks;
}
