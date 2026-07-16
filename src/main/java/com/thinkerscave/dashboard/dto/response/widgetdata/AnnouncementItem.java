package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AnnouncementItem {
    private String title;
    private String summary;
    private String category;
    private LocalDate publishedAt;
    private boolean pinned;
}
