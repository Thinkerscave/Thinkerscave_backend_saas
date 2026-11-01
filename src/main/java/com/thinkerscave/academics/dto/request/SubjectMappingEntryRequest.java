package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.SubjectRequirement;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** One subject's curriculum settings inside an academic group. */
@Getter
@Setter
public class SubjectMappingEntryRequest {

    @NotNull(message = "Subject is mandatory")
    private Long subjectId;

    private SubjectRequirement requirement = SubjectRequirement.COMPULSORY;

    @NotNull(message = "Weekly periods are mandatory")
    @Min(value = 1, message = "Weekly periods must be at least 1")
    @Max(value = 40, message = "Weekly periods cannot exceed 40")
    private Short weeklyPeriods;

    private SubjectTimetablePreference timetablePreference = SubjectTimetablePreference.ANY;

    @Size(max = 60)
    private String choiceGroup;

    @Min(value = 1, message = "Consecutive periods must be at least 1")
    @Max(value = 10, message = "Consecutive periods cannot exceed 10")
    private Short consecutivePeriods = 1;
}
