package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.DayOfWeek;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimetableWorkingDayRequest {

    @NotNull
    private DayOfWeek dayOfWeek;

    @NotNull
    private Boolean working;
}
