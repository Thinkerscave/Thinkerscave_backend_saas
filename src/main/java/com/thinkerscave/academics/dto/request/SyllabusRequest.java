package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SyllabusRequest {

    @NotNull(message = "Academic year ID is mandatory")
    private Long academicYearId;

    @NotNull(message = "Class ID is mandatory")
    private Long classId;

    @NotNull(message = "Subject ID is mandatory")
    private Long subjectId;

    @NotBlank(message = "Title is mandatory")
    @Size(max = 150)
    private String title;

    private String remarks;
}
