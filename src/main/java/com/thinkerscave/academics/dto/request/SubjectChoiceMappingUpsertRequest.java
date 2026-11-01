package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SubjectChoiceMappingUpsertRequest {

    @NotNull(message = "Choice group is mandatory")
    private Long choiceGroupId;

    @NotNull(message = "Weekly periods are mandatory")
    @Min(value = 1, message = "Weekly periods must be at least 1")
    @Max(value = 40, message = "Weekly periods cannot exceed 40")
    private Short weeklyPeriods;

    private SubjectTimetablePreference timetablePreference = SubjectTimetablePreference.ANY;

    /** Effective weekly lesson periods by subject alternative; omitted subjects inherit the shared workload. */
    private Map<@NotNull Long, @NotNull @Min(1) @Max(40) Short> subjectPeriods;
}
