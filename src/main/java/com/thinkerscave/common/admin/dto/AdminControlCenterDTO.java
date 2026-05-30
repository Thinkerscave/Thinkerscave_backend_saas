package com.thinkerscave.common.admin.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdminControlCenterDTO(
        Instant generatedAt,
        List<AdminSectionDTO> adminSections,
        List<KpiDTO> kpis,
        OrganizationSummaryDTO organizationSummary,
        List<OrganizationDTO> organizations,
        List<BranchDTO> branches,
        List<RoleDTO> roles,
        List<UserAccessDTO> users,
        List<MenuSectionDTO> menuSections,
        List<PermissionMatrixRowDTO> permissionMatrix,
        MonitoringDTO monitoring,
        List<ActivityDTO> activities,
        List<AuditEventDTO> auditLogs,
        List<SecurityEventDTO> securityEvents,
        List<SystemEventDTO> systemEvents) {

    public record AdminSectionDTO(
            Long id,
            String code,
            String label,
            String description,
            String route,
            String icon,
            Integer order) {
    }

    public record KpiDTO(
            String key,
            String label,
            String value,
            String helper,
            String icon,
            String tone) {
    }

    public record OrganizationSummaryDTO(
            long schools,
            long colleges,
            long branches,
            long departments,
            long students,
            long staff,
            long parents,
            long activeMemberships) {
    }

    public record OrganizationDTO(
            Long orgId,
            String orgCode,
            String orgName,
            String brandName,
            String orgType,
            String city,
            String state,
            String tenantId,
            String subscriptionType,
            Boolean active,
            String ownerName,
            String ownerEmail,
            LocalDate establishDate,
            long branches,
            long students,
            long staff,
            long activeUsers,
            long storageUsedMb,
            long storageLimitMb,
            long apiUsageToday,
            int healthScore) {
    }

    public record BranchDTO(
            Long id,
            String branchCode,
            String branchName,
            String location,
            Boolean active,
            Long organizationId,
            long staff,
            long students) {
    }

    public record RoleDTO(
            Long roleId,
            String roleCode,
            String roleName,
            String description,
            Boolean active,
            String roleType,
            long users,
            long permissionAssignments) {
    }

    public record UserAccessDTO(
            Long id,
            String userCode,
            String fullName,
            String userName,
            String email,
            Boolean blocked,
            Boolean firstTimeLogin,
            Boolean emailVerified,
            LocalDate lastLoginDate,
            List<String> roles,
            List<String> organizations,
            String invitationStatus) {
    }

    public record MenuSectionDTO(
            Long menuId,
            String menuCode,
            String name,
            String icon,
            Boolean active,
            long activePages,
            long totalPages,
            List<AdminSectionDTO> pages) {
    }

    public record PermissionMatrixRowDTO(
            Long roleId,
            String roleCode,
            String roleName,
            List<PermissionCellDTO> permissions) {
    }

    public record PermissionCellDTO(
            Long privilegeId,
            String privilegeName,
            long assignedPages,
            long totalPages,
            boolean assigned) {
    }

    public record MonitoringDTO(
            int healthScore,
            String databaseStatus,
            long openEvents,
            long criticalEvents,
            long failedSecurityEvents,
            List<MonitoringWidgetDTO> widgets,
            List<SystemEventDTO> jobs,
            List<SystemEventDTO> notifications,
            List<SystemEventDTO> dataIntegrity) {
    }

    public record MonitoringWidgetDTO(
            String key,
            String label,
            String value,
            String helper,
            String status,
            String icon,
            String tone) {
    }

    public record ActivityDTO(
            String title,
            String description,
            String actor,
            String icon,
            String tone,
            Instant occurredAt) {
    }

    public record AuditEventDTO(
            Long id,
            String eventType,
            String action,
            String entityType,
            String entityId,
            String actorUsername,
            String sourceIp,
            String summary,
            String changes,
            Instant occurredAt) {
    }

    public record SecurityEventDTO(
            Long id,
            String eventCode,
            String username,
            String sourceIp,
            boolean success,
            String severity,
            String message,
            Instant occurredAt) {
    }

    public record SystemEventDTO(
            Long id,
            Long organizationId,
            String tenantCode,
            String category,
            String component,
            String eventCode,
            String title,
            String message,
            String severity,
            String status,
            String metricName,
            Double metricValue,
            String metricUnit,
            Boolean resolved,
            Instant occurredAt) {
    }
}