package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.TimetableSlotKind;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
public class SchedulePatternResponse {

    private Long schedulePatternId;
    private Long academicYearId;
    private String name;
    private String description;
    private Boolean active;
    private int teachingSlotCount;
    private int breakSlotCount;
    private LocalTime dayStartTime;
    private LocalTime dayEndTime;
    private int totalDurationMinutes;
    private int assignedGroupCount;
    private List<AssignedGroup> assignedGroups;
    private List<Slot> slots;

    @Getter
    @Setter
    @Builder
    public static class Slot {
        private Long periodId;
        private Integer slotOrder;
        private String name;
        private TimetableSlotKind slotType;
        private LocalTime startTime;
        private LocalTime endTime;
        private int durationMinutes;
    }

    @Getter
    @Setter
    @Builder
    public static class AssignedGroup {
        private Long academicGroupId;
        private String academicGroupName;
        private Long sectionId;
        private String sectionName;
    }
}
