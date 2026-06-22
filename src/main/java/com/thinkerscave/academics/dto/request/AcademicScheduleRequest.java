package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AcademicScheduleRequest {

    @NotNull(message = "Academic year ID is mandatory")
    private Long academicYearId;

    @NotBlank(message = "Schedule name is mandatory")
    @Size(max = 100)
    private String scheduleName;

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    private LocalDate endDate;

    private String remarks;
}
