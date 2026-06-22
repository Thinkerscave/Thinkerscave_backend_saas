package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassScheduleAssignmentRequest {

    @NotNull(message = "Class ID is mandatory")
    private Long classId;

    private Long sectionId;

    @NotNull(message = "Schedule ID is mandatory")
    private Long scheduleId;

    @NotNull(message = "Template ID is mandatory")
    private Long templateId;

    private String remarks;
}
