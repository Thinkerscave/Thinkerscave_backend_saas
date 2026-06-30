package com.thinkerscave.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(description = "An attendance freeze period")
public class AttendanceFreezeResponse {

    private Long freezeId;
    private Long organizationId;
    private LocalDate freezeFromDate;
    private LocalDate freezeToDate;
    private String reason;
    private Boolean active;
}
