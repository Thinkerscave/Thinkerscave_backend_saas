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
    private Long academicGroupId;
    private String academicGroupName;
    private Long sectionId;
    private String sectionName;
    private Long staffId;
    private String staffName;
    private Boolean editable;
    private java.time.LocalDate weekStartDate;
    private java.util.List<DayDate> weekDates;

    @Getter
    @Setter
    @Builder
    public static class DayDate {
        private DayOfWeek dayOfWeek;
        private java.time.LocalDate date;
    }
}
