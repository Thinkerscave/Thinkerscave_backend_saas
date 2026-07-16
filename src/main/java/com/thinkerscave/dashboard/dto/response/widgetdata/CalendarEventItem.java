package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CalendarEventItem {
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String eventType;
    private boolean allDay;
}
