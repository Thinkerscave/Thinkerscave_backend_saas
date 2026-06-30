package com.thinkerscave.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Bulk period-wise student attendance marking")
public class MarkPeriodAttendanceRequest {

    @NotNull(message = "Academic year ID is required")
    private Long academicYearId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    private Long sectionId;

    @NotNull(message = "Period ID is required")
    private Long periodId;

    private Integer periodNumber;
    private String periodName;
    private Long subjectId;
    private String subjectName;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotEmpty(message = "At least one student entry is required")
    @Valid
    private List<StudentAttendanceEntry> entries;
}
