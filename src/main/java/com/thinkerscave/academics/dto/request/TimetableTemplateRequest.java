package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimetableTemplateRequest {

    @NotNull(message = "Schedule ID is mandatory")
    private Long scheduleId;

    @NotBlank(message = "Template name is mandatory")
    @Size(max = 100)
    private String templateName;

    private String remarks;
}
