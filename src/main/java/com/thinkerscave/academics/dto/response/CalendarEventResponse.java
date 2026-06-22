package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CalendarEventResponse {
    private Long eventId;
    private Long academicYearId;
    private String title;
    private String eventType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean allDay;
    private String description;
    private Boolean active;
}
