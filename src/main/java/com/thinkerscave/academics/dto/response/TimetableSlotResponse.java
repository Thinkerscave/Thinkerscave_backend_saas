package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
public class TimetableSlotResponse {
    private Long slotId;
    private String dayOfWeek;
    private Integer periodNumber;
    private String periodName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String subjectName;
    private Long teacherId;
    private Boolean active;
}
