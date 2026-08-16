package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.AcademicStage;
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

    @NotBlank(message = "Class name is mandatory")
    @Size(max = 100, message = "Class name cannot exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Class code cannot exceed 50 characters")
    private String code;

    @NotNull(message = "Academic stage is mandatory")
    private AcademicStage stage;

    private Integer displayOrder;
}
