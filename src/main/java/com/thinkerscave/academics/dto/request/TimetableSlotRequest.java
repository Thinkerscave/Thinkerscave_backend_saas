package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimetableSlotRequest {

    @NotNull(message = "Class ID is mandatory")
    private Long classId;

    private Long sectionId;

    @NotBlank(message = "Day of week is mandatory")
    private String dayOfWeek;

    @NotNull(message = "Period template ID is mandatory")
    private Long periodTemplateId;

    @NotNull(message = "Subject assignment ID is mandatory")
    private Long subjectAssignmentId;
}
