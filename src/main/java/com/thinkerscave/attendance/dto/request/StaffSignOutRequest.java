package com.thinkerscave.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Staff self sign-out. staffId is optional; when omitted the authenticated staff profile is used.")
public class StaffSignOutRequest {

    @Schema(description = "Optional. Must match the authenticated staff member when provided.")
    private Long staffId;

    private String remarks;
}
