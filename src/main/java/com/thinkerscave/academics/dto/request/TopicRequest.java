package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicRequest {

    @NotNull(message = "Topic number is mandatory")
    private Integer topicNumber;

    @NotBlank(message = "Topic name is mandatory")
    private String topicName;

    private Integer estimatedHours;
    private Integer displayOrder;
    private String remarks;
}
