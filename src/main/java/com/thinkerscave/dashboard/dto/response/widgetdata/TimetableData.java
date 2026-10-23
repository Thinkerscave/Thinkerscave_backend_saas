package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TimetableData {
    private String dayLabel;
    private LocalDate date;
    private List<TimetableSlotItem> slots;
}
