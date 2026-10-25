package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.AcademicStage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AcademicGroupRequest {

    @NotNull(message = "Academic year ID is mandatory")
    private Long academicYearId;

    @NotBlank(message = "Academic Group name is mandatory")
    @Size(max = 100, message = "Academic Group name cannot exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Academic Group code cannot exceed 50 characters")
    private String code;

    /** Optional institution-specific classification (school stage, program type, batch type). */
    private AcademicStage stage;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Integer displayOrder;

    /** Optional sections created in the same transaction as the group. */
    @Valid
    private List<AcademicSectionRequest> sections;
}
