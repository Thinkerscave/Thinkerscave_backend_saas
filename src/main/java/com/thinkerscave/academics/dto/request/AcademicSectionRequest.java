package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcademicSectionRequest {

    @NotBlank(message = "Section name is mandatory")
    @Size(max = 50, message = "Section name cannot exceed 50 characters")
    private String name;

    @Size(max = 50, message = "Section code cannot exceed 50 characters")
    private String code;

    @Positive(message = "Capacity must be greater than zero")
    private Integer capacity;

    private Integer displayOrder;

    private Long defaultResourceId;

    /** Optional class teacher assignment on create/update. */
    private Long classTeacherStaffId;
}
