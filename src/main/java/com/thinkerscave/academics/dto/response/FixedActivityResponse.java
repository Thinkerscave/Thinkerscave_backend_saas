package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
public class FixedActivityResponse {

    private Long fixedActivityId;
    private Long academicYearId;
    private String name;
    private String description;
    private String activityType;
    private Boolean appliesToAllGroups;
    private LocalTime startTime;
    private LocalTime endTime;
    private String recurrence;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private Set<DayOfWeek> days;
    private List<Group> groups;

    @Getter
    @Setter
    @Builder
    public static class Group {
        private Long academicGroupId;
        private String academicGroupName;
        private Long sectionId;
        private String sectionName;
    }
}
