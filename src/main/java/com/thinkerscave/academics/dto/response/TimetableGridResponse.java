package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class TimetableGridResponse {

    private Long timetableVersionId;
    private String view;
    private List<TimetablePeriodResponse> periods;
    private List<DayOfWeek> workingDays;
    private List<TimetableCellResponse> cells;
}
