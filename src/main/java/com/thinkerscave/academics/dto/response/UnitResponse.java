package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class UnitResponse {
    private Long unitId;
    private Integer unitNumber;
    private String unitName;
    private Integer estimatedHours;
    private Integer displayOrder;
    private Boolean active;
    private List<ChapterResponse> chapters;
}
