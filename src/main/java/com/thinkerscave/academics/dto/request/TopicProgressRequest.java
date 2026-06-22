package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TopicProgressRequest {

    @NotNull(message = "Teacher ID is mandatory")
    private Long teacherId;

    @NotBlank(message = "Status is mandatory")
    private String status;

    private LocalDate completionDate;
    private String remarks;
}
