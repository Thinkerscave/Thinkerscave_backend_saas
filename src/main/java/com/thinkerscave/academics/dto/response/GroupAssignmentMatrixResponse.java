package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
public class GroupAssignmentMatrixResponse {

    private Long academicYearId;
    private List<DayOfWeek> days;
    private List<Row> rows;

    @Getter
    @Setter
    @Builder
    public static class Row {
        private Long academicGroupId;
        private String academicGroupName;
        private String academicGroupCode;
        private Long sectionId;
        private String sectionName;
        private Boolean active;
        private List<Cell> cells;
    }

    @Getter
    @Setter
    @Builder
    public static class Cell {
        private DayOfWeek dayOfWeek;
        private Long groupScheduleAssignmentId;
        private Long schedulePatternId;
        private String schedulePatternName;
        private LocalTime startTime;
        private LocalTime endTime;
        private Boolean active;
        private String effectiveFrom;
        private String effectiveTo;
    }
}
