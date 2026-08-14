package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.SubjectCategory;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectRequest {

    @NotNull(message = "Academic year ID is mandatory")
    private Long academicYearId;

    @NotBlank(message = "Subject name is mandatory")
    @Size(max = 150, message = "Subject name cannot exceed 150 characters")
    private String name;

    @Size(max = 50, message = "Subject code cannot exceed 50 characters")
    private String code;

    @NotNull(message = "Subject category is mandatory")
    private SubjectCategory category;

    @NotNull(message = "Default weekly periods is mandatory")
    @Positive(message = "Default weekly periods must be greater than zero")
    private Short defaultWeeklyPeriods;

    @NotNull(message = "Timetable preference is mandatory")
    private SubjectTimetablePreference timetablePreference = SubjectTimetablePreference.ANY;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
