package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CalendarEventItem {
    private Long eventId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String startTime;
    private String endTime;
    private String location;
    private String eventType;
    private boolean allDay;
}
