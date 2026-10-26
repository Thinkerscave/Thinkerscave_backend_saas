package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.DayOfWeek;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Assign one pattern to multiple group/section scheduling units on the selected days. */
@Getter
@Setter
public class GroupScheduleAssignmentRequest {

    @NotNull
    private Long schedulePatternId;

    /** Each selected group (section optional – null means the whole group). */
    private List<Unit> units = new ArrayList<>();

    private List<Long> academicGroupIds = new ArrayList<>();

    private List<DayOfWeek> days = new ArrayList<>();

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Getter
    @Setter
    public static class Unit {
        private Long academicGroupId;
        private Long sectionId;
    }
}
