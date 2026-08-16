package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.CalendarAudienceType;
import com.thinkerscave.academics.enums.CalendarEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class AcademicCalendarEventRequest {

    @NotNull(message = "Academic year ID is mandatory")
    private Long academicYearId;

    @NotBlank(message = "Title is mandatory")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Event type is mandatory")
    private CalendarEventType eventType;

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    private LocalDate endDate;

    @NotNull(message = "All-day flag is mandatory")
    private Boolean allDay = true;

    private LocalTime startTime;

    private LocalTime endTime;

    @Size(max = 255, message = "Location cannot exceed 255 characters")
    private String location;

    @NotNull(message = "Audience type is mandatory")
    private CalendarAudienceType audienceType;

    private List<Long> classIds;

    private List<Long> sectionIds;

    /** When true on create, event is saved as PUBLISHED. */
    private Boolean publish;
}
