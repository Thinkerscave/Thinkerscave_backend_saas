package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class TimetableSlotItem {
    private Integer periodNumber;
    private String periodName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String subjectName;
    private String teacherName;
    private String className;
    private String roomLabel;
}
