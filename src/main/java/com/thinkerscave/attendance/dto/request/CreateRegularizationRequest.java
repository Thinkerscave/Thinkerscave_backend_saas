package com.thinkerscave.attendance.dto.request;

import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "Create a staff attendance regularization request")
public class CreateRegularizationRequest {

    @NotNull
    private LocalDate attendanceDate;

    @NotNull
    private StaffAttendanceStatus requestedStatus;

    private LocalDateTime requestedSignInTime;

    private LocalDateTime requestedSignOutTime;

    @NotBlank
    private String reason;

    private String remarks;
}
