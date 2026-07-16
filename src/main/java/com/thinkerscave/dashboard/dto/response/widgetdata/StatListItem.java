package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatListItem {
    private String label;
    private String value;
    private String icon;
    private String tone;
    private String secondaryLabel;
}
