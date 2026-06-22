package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TeacherArrangementRequest {

    @NotNull(message = "Timetable slot ID is mandatory")
    private Long slotId;

    @NotNull(message = "Absent teacher ID is mandatory")
    private Long absentTeacherId;

    @NotNull(message = "Substitute teacher ID is mandatory")
    private Long substituteTeacherId;

    @NotNull(message = "Arrangement date is mandatory")
    private LocalDate arrangementDate;

    private String reason;
}
