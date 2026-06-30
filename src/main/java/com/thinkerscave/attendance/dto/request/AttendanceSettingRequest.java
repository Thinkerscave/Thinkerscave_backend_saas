package com.thinkerscave.attendance.dto.request;

import com.thinkerscave.attendance.enums.AttendanceMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalTime;

@Data
@Schema(description = "Attendance configuration settings for an organization")
public class AttendanceSettingRequest {

    private AttendanceMode attendanceMode;

    @Schema(description = "Time after which students are marked Late (e.g. 08:15)")
    private LocalTime lateAfterTime;

    private LocalTime windowStartTime;
    private LocalTime windowEndTime;

    private Boolean allowCopyPrevious;

    @Min(0) @Max(100)
    private Integer minStudentAttendancePercent;

    @Min(0) @Max(100)
    private Integer studentAlertThresholdPercent;

    private Boolean sendSmsOnAbsent;
    private Boolean sendEmailOnAbsent;

    @Min(1) @Max(24)
    private Integer minStaffWorkingHours;

    @Min(0) @Max(120)
    private Integer staffLateGraceMinutes;

    @Min(0)
    @Schema(description = "0 = no automatic freeze; N = lock attendance after N days")
    private Integer freezeAfterDays;
}
