package com.thinkerscave.student.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimelineDTO {

    private Long timelineId;

    private String eventType;

    private String title;

    private String description;

    private Instant createdDate;

    private String createdBy;
}