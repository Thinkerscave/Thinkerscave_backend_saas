package com.thinkerscave.common.student.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StudentTimelineEntry {
    private String action;
    private String description;
    private String performedBy;
    private Instant performedAt;
    private String icon;
    private String tone;
}
