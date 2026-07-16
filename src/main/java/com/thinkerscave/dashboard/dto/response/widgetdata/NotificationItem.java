package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class NotificationItem {
    private String title;
    private String message;
    private LocalDate date;
    private String category;
    private boolean pinned;
}
