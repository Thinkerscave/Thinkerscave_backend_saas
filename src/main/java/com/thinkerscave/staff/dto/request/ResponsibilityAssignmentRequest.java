package com.thinkerscave.staff.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ResponsibilityAssignmentRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    @NotNull(message = "Responsibility ID is required")
    private Long responsibilityId;

    @Size(max = 200)
    private String scope;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String remarks;
}
