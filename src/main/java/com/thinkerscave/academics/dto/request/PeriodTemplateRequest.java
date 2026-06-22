package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class PeriodTemplateRequest {

    @NotNull(message = "Period number is mandatory")
    private Integer periodNumber;

    @NotBlank(message = "Period name is mandatory")
    private String periodName;

    @NotNull(message = "Start time is mandatory")
    private LocalTime startTime;

    @NotNull(message = "End time is mandatory")
    private LocalTime endTime;

    @NotBlank(message = "Period type is mandatory")
    private String periodType;

    private Integer displayOrder;
}
