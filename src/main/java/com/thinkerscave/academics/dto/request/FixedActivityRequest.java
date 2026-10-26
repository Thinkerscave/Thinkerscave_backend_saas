package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.DayOfWeek;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class FixedActivityRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 40)
    private String activityType = "GENERAL";

    private Boolean appliesToAllGroups = true;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private String recurrence = "WEEKLY";

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Boolean active = true;

    private Set<DayOfWeek> days = new LinkedHashSet<>();

    private List<Unit> groups = new ArrayList<>();

    @Getter
    @Setter
    public static class Unit {
        private Long academicGroupId;
        private Long sectionId;
    }
}
