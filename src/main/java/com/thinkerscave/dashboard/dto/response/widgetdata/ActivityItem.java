package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ActivityItem {
    private String title;
    private String description;
    private String actorName;
    private Instant occurredAt;
    private String icon;
}
