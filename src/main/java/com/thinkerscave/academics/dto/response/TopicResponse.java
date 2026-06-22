package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TopicResponse {
    private Long topicId;
    private Integer topicNumber;
    private String topicName;
    private Integer estimatedHours;
    private Integer displayOrder;
    private Boolean active;
    private String coverageStatus;
}
