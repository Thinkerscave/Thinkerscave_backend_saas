package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChartData {
    /** "line" | "bar" | "donut" | "area". */
    private String chartType;
    private List<String> labels;
    private List<ChartSeries> series;
    private String unit;
}
