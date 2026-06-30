package com.thinkerscave.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Create an attendance freeze period")
public class AttendanceFreezeRequest {

    @NotNull(message = "Freeze from date is required")
    private LocalDate freezeFromDate;

    @NotNull(message = "Freeze to date is required")
    private LocalDate freezeToDate;

    @Schema(description = "Reason for freezing attendance")
    private String reason;
}
