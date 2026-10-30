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
    /**
     * Periods that actually belong to each working day. Different days may follow different
     * schedule patterns (for example a Saturday morning pattern), so the grid must not merge
     * them into a single column set.
     */
    private java.util.Map<DayOfWeek, List<TimetablePeriodResponse>> periodsByDay;
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
