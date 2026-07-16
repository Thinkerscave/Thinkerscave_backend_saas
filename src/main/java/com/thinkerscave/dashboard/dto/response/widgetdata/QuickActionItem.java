package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuickActionItem {
    private String label;
    private String icon;
    private String route;
    private String tone;
}
