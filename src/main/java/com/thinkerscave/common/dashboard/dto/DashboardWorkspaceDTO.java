package com.thinkerscave.common.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DashboardWorkspaceDTO(
        UserContext context,
        List<Widget> widgets,
        List<Kpi> kpis,
        List<QuickAction> quickActions,
        List<Priority> priorities,
        List<Approval> pendingApprovals,
        List<Activity> recentActivities,
        List<Alert> smartAlerts,
        List<Shortcut> moduleShortcuts,
        SearchMeta search) {

    public record UserContext(
            Long userId,
            String username,
            String displayName,
            String primaryRoleCode,
            String primaryRoleName,
            List<String> roleCodes,
            Long organizationId,
            String organizationName,
            String tenantId,
            String welcomeTitle,
            String focusMessage) {
    }

    public record Widget(
            String key,
            String type,
            String title,
            String subtitle,
            String icon,
            String route,
            String section,
            Integer displayOrder) {
    }

    public record Kpi(
            String key,
            String label,
            String value,
            String helper,
            String tone,
            String icon,
            String route) {
    }

    public record QuickAction(
            String key,
            String label,
            String description,
            String icon,
            String route,
            String tone,
            boolean enabled) {
    }

    public record Priority(
            String key,
            String title,
            String description,
            String dueLabel,
            String tone,
            String icon,
            String route,
            String entityType,
            String entityId) {
    }

    public record Approval(
            String key,
            String title,
            String description,
            String requester,
            String status,
            String tone,
            String route,
            String entityType,
            String entityId) {
    }

    public record Activity(
            String key,
            String title,
            String description,
            String actor,
            Instant occurredAt,
            String tone,
            String icon,
            String route) {
    }

    public record Alert(
            String key,
            String title,
            String description,
            String severity,
            String tone,
            String icon,
            String route,
            String entityType,
            String entityId) {
    }

    public record Shortcut(
            String key,
            String label,
            String description,
            String icon,
            String route,
            Long count,
            String tone) {
    }

    public record SearchMeta(
            String placeholder,
            List<String> categories) {
    }
}