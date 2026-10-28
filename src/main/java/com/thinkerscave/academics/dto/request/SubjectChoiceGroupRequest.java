package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubjectChoiceGroupRequest {

    @NotBlank(message = "Choice group name is required")
    @Size(max = 100, message = "Choice group name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Subject type is required")
    private SubjectType subjectType;

    @NotNull(message = "Select at least two subjects")
    private List<@NotNull Long> subjectIds;
}
