package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcademicClassRequest {

    @NotNull(message = "Academic year ID is mandatory")
    private Long academicYearId;

    @NotBlank(message = "Class code is mandatory")
    @Size(max = 30)
    private String classCode;

    @NotBlank(message = "Class name is mandatory")
    @Size(max = 100)
    private String className;

    @NotBlank(message = "Academic stage is mandatory")
    private String academicStage;

    private Integer displayOrder;
    private String remarks;
}
