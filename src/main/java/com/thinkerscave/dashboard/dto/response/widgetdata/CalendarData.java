package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Shared payload shape for both the CALENDAR and EVENTS widget types. */
@Data
@Builder
public class CalendarData {
    private List<CalendarEventItem> items;
}
