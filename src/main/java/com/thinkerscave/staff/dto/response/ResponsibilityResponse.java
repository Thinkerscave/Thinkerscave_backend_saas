package com.thinkerscave.staff.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ResponsibilityResponse {

    private Long responsibilityId;
    private String responsibilityCode;
    private String responsibilityName;
    private String description;
    private Integer displayOrder;
    private Boolean systemDefined;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}
