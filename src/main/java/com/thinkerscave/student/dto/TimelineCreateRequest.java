package com.thinkerscave.student.dto;

import com.thinkerscave.student.enums.StudentTimelineEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimelineCreateRequest {

    @NotNull
    private StudentTimelineEventType eventType;

    @NotBlank
    private String title;

    private String description;
}
