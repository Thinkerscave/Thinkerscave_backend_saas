package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcademicResourceRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotNull
    private ResourceType resourceType;

    @Positive
    private Integer capacity;
}
