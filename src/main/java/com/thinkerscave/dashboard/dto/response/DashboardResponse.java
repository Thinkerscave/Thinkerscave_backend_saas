package com.thinkerscave.dashboard.dto.response;

import com.thinkerscave.dashboard.enums.DashboardType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Full payload for {@code GET /api/v1/dashboard/workspace}. The frontend
 * shell renders {@link #widgets} in order using its widget registry; it
 * never branches on {@link #dashboardType} beyond display purposes
 * (e.g. analytics, page title).
 */
@Data
@Builder
public class DashboardResponse {

    private DashboardType dashboardType;

    private Instant generatedAt;

    private List<WidgetDTO<?>> widgets;
}
