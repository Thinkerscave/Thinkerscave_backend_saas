package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloneAcademicYearRequest {

    @NotBlank(message = "New year code is mandatory")
    private String newYearCode;

    @NotBlank(message = "New year name is mandatory")
    private String newYearName;

    private boolean copyClasses = true;
    private boolean copySections = true;
    private boolean copySubjects = true;
    private boolean copyTeacherAllocations = false;
    private boolean copySchedules = true;
    private boolean copyTemplates = true;
    private boolean copySyllabus = false;
}
