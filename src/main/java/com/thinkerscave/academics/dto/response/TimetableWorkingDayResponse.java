package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TimetableWorkingDayResponse {

    private Long timetableWorkingDayId;
    private DayOfWeek dayOfWeek;
    private Boolean working;
}
