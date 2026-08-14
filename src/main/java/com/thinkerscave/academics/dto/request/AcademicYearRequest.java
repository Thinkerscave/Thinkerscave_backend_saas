package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.AcademicYearPattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AcademicYearRequest {

    @NotBlank(message = "Academic year name is mandatory")
    @Size(max = 50, message = "Academic year name cannot exceed 50 characters")
    private String name;

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    private LocalDate endDate;

    @NotNull(message = "Academic pattern is mandatory")
    private AcademicYearPattern pattern = AcademicYearPattern.ANNUAL;
}
