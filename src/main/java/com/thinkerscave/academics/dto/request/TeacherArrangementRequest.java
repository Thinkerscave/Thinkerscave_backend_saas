package com.thinkerscave.academics.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TeacherArrangementRequest(
        @NotNull Long academicYearId,
        @NotNull LocalDate absenceDate,
        @NotNull Long absentTeacherId,
        @NotBlank @Size(max = 500) String reason,
        @NotEmpty List<@Valid Slot> slots) {

    public record Slot(@NotNull Long timetableEntryId, @NotNull Long substituteStaffId) {}
}