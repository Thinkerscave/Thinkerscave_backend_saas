package com.thinkerscave.attendance.dto.request;

import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Update a single student attendance record")
public class UpdateStudentAttendanceRequest {

    @NotNull(message = "Status is required")
    private StudentAttendanceStatus status;

    private String remarks;
}
