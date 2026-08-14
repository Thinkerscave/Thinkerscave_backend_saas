package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.TimetableSlotKind;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
public class TimetablePeriodResponse {

    private Long timetablePeriodId;
    private Short periodNumber;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private TimetableSlotKind slotKind;
}
