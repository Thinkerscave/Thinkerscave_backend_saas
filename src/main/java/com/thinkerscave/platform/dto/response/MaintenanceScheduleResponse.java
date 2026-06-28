package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MaintenanceScheduleResponse {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private String title;
    private String description;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Boolean planned;
    private Boolean notificationSent;
    private Boolean completed;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
