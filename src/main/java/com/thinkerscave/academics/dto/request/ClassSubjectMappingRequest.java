package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassSubjectMappingRequest {

    @NotNull(message = "Subject ID is mandatory")
    private Long subjectId;

    /** When false, mapping is deactivated (excluded from class). */
    @NotNull
    private Boolean included = true;

    @Positive(message = "Weekly periods must be greater than zero")
    private Short weeklyPeriods;

    private SubjectTimetablePreference timetablePreference;
}
