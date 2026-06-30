package com.thinkerscave.attendance.dto.response;

import com.thinkerscave.attendance.enums.AttendanceMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
@Schema(description = "Attendance settings for an organization")
public class AttendanceSettingResponse {

    private Long settingId;
    private Long organizationId;
    private AttendanceMode attendanceMode;
    private LocalTime lateAfterTime;
    private LocalTime windowStartTime;
    private LocalTime windowEndTime;
    private Boolean allowCopyPrevious;
    private Integer minStudentAttendancePercent;
    private Integer studentAlertThresholdPercent;
    private Boolean sendSmsOnAbsent;
    private Boolean sendEmailOnAbsent;
    private Integer minStaffWorkingHours;
    private Integer staffLateGraceMinutes;
    private Integer freezeAfterDays;
    private Boolean active;
}
