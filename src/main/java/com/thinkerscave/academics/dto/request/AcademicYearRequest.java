package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AcademicYearRequest {

    @NotBlank(message = "Year code is mandatory")
    @Size(max = 20, message = "Year code cannot exceed 20 characters")
    private String yearCode;

    @NotBlank(message = "Year name is mandatory")
    @Size(max = 100, message = "Year name cannot exceed 100 characters")
    private String yearName;

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    private LocalDate endDate;

    private String remarks;
}
