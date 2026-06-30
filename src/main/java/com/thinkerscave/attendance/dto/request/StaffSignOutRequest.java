package com.thinkerscave.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Staff self sign-out")
public class StaffSignOutRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    private String remarks;
}
