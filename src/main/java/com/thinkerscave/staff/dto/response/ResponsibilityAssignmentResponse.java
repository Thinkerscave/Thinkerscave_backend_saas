package com.thinkerscave.staff.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ResponsibilityAssignmentResponse {

    private Long assignmentId;
    private Long staffId;
    private String staffName;
    private String staffCode;
    private Long responsibilityId;
    private String responsibilityCode;
    private String responsibilityName;
    private String scope;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
}
