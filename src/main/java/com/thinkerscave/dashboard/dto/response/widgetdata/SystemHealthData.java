package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SystemHealthData {
    private String overallStatus;
    private List<StatListItem> checks;
}
