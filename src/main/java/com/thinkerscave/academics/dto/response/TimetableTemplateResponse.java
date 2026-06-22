package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TimetableTemplateResponse {
    private Long templateId;
    private Long scheduleId;
    private String scheduleName;
    private String templateName;
    private Boolean active;
}
