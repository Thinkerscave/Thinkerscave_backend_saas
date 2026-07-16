package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AttendanceSummaryData {
    private long presentCount;
    private long absentCount;
    private long lateCount;
    private long totalCount;
    private double percentage;
    private LocalDate date;
}
