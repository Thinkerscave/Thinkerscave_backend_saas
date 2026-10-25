package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectRequest {

    @NotBlank(message = "Subject name is mandatory")
    @Size(max = 150, message = "Subject name cannot exceed 150 characters")
    private String name;

    @Size(max = 50, message = "Subject code cannot exceed 50 characters")
    private String code;

    @NotNull(message = "Subject type is mandatory")
    private SubjectType subjectType;

    @Size(max = 300, message = "Description cannot exceed 300 characters")
    private String description;
}
