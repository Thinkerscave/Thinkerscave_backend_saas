package com.thinkerscave.staff.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponsibilityRequest {

    @NotBlank(message = "Responsibility code is required")
    @Size(max = 30)
    private String responsibilityCode;

    @NotBlank(message = "Responsibility name is required")
    @Size(max = 150)
    private String responsibilityName;

    @Size(max = 500)
    private String description;

    private Integer displayOrder;

    private String remarks;
}
