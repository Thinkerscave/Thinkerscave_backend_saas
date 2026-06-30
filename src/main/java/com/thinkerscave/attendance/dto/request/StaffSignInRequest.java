package com.thinkerscave.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Staff self sign-in")
public class StaffSignInRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    @Schema(description = "Optional device/location info")
    private String remarks;
}
