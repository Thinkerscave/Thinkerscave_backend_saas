package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CalendarEventRequest {

    @NotBlank(message = "Event title is mandatory")
    @Size(max = 150)
    private String title;

    @NotBlank(message = "Event type is mandatory")
    private String eventType;

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;

    private LocalDate endDate;
    private String description;
    private Boolean allDay = true;

    private Long academicYearId;
}
