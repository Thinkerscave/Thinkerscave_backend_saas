package com.thinkerscave.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Attendance report query parameters")
public class AttendanceReportRequest {

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    private Long classId;
    private Long sectionId;
    private Long academicYearId;

    @Schema(description = "Defaulter threshold percentage — students below this are flagged", example = "75")
    private Integer defaulterThreshold = 75;
}
