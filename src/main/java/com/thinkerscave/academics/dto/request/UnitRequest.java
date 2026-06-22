package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnitRequest {

    @NotNull(message = "Unit number is mandatory")
    private Integer unitNumber;

    @NotBlank(message = "Unit name is mandatory")
    private String unitName;

    private Integer estimatedHours;
    private Integer displayOrder;
    private String remarks;
}
