package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClassTeacherAssignmentRequest {

    @NotNull(message = "Academic year ID is mandatory")
    private Long academicYearId;

    @NotNull(message = "Class ID is mandatory")
    private Long classId;

    private Long sectionId;

    @NotNull(message = "Teacher ID is mandatory")
    private Long teacherId;

    private LocalDate effectiveFrom;
    private String remarks;
}
