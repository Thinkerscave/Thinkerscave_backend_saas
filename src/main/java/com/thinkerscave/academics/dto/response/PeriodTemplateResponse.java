package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
public class PeriodTemplateResponse {
    private Long periodTemplateId;
    private Long templateId;
    private Integer periodNumber;
    private String periodName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String periodType;
    private Integer displayOrder;
    private Boolean active;
}
