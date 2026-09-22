package com.thinkerscave.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Staff self sign-in. staffId is optional; when omitted the authenticated staff profile is used.")
public class StaffSignInRequest {

    @Schema(description = "Optional. Must match the authenticated staff member when provided.")
    private Long staffId;

    @Schema(description = "Optional device/location info")
    private String remarks;
}
