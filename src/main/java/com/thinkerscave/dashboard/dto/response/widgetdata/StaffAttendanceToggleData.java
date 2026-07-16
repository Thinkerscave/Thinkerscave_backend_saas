package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StaffAttendanceToggleData {
    private Long staffId;
    private boolean signedIn;
    private boolean signedOut;
    private LocalDateTime signInTime;
    private LocalDateTime signOutTime;
    private Integer workingMinutesSoFar;
    private String status;
}
