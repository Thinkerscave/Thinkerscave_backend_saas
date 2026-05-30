package com.thinkerscave.common.dashboard.dto;

import java.util.List;
import java.util.Map;

public record DashboardSearchDTO(
        String query,
        List<Result> results,
        List<String> supportedCategories) {

    public record Result(
            String key,
            String entityType,
            String entityId,
            String title,
            String subtitle,
            String detail,
            String icon,
            String route,
            String tone,
            Map<String, Object> metadata) {
    }
}