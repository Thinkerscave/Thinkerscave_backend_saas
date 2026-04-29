package com.thinkerscave.common.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SectionDTO {
    private Long sectionId;

    @NotBlank(message = "Section name is mandatory")
    private String sectionName;

    @NotNull(message = "Class ID is mandatory")
    private Long classId;
}
