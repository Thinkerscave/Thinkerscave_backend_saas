package com.thinkerscave.common.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassDTO {
    private Long classId;

    @NotBlank(message = "Class name is mandatory")
    private String className;
}
