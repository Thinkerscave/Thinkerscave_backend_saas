package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiItem {
    private String label;
    private String value;
    private String icon;
    private String tone;
    /** Positive, negative or null when trend is not applicable. */
    private Double trendPercent;
    private String trendLabel;
    @Builder.Default
    private boolean sample = false;
}
