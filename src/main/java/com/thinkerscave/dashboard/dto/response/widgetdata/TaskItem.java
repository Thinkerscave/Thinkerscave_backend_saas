package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskItem {
    private String title;
    private String dueLabel;
    /** "high" | "medium" | "low". */
    private String priority;
    private boolean completed;
    private String link;
    @Builder.Default
    private boolean sample = false;
}
