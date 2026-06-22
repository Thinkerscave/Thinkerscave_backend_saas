package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcademicSectionRequest {

    @NotNull(message = "Class ID is mandatory")
    private Long classId;

    @NotBlank(message = "Section name is mandatory")
    @Size(max = 20)
    private String sectionName;

    private Integer capacity;
    private String remarks;
}
