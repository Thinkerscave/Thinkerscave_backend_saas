package com.thinkerscave.dashboard.service.provider;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.audit.entity.AuditLog;
import com.thinkerscave.audit.repository.AuditLogRepository;
import com.thinkerscave.dashboard.dto.response.WidgetDTO;
import com.thinkerscave.dashboard.dto.response.widgetdata.*;
import com.thinkerscave.dashboard.enums.DataMode;
import com.thinkerscave.dashboard.enums.WidgetType;
import com.thinkerscave.dashboard.service.SampleWidgetFactory;
import com.thinkerscave.dashboard.util.ChartBucketUtil;
import com.thinkerscave.dashboard.util.RoleLabels;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.enums.OrganizationStatus;
import com.thinkerscave.platform.enums.SubscriptionStatus;
import com.thinkerscave.platform.repository.CustomerRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.OrganizationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Platform-wide dashboard for ThinkersCave Super Admins. Unlike the other
 * providers, queries here intentionally aggregate across every
 * organization — this is the one role allowed to see cross-tenant data.
 */
@Component
@RequiredArgsConstructor
public class SuperAdminDashboardProvider extends AbstractDashboardWidgetProvider implements DashboardWidgetProvider {

    private final OrganizationRepository organizationRepository;
    private final CustomerRepository customerRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SampleWidgetFactory sampleWidgetFactory;

    @Override
    public List<WidgetDTO<?>> getWidgets(User user) {
        return List.of(
                welcomeHeader(user),
                kpiGrid(),
                quickActions(),
                organizationGrowthChart(),
                newOrganizationsChart(),
                userGrowthChart(),
                systemHealth(),
                recentOrganizations(),
                recentActivity(),
                topOrganizations(),
                supportTicketsPreview()
        );
    }

    private WidgetDTO<WelcomeHeaderData> welcomeHeader(User user) {
        return safeWidget("welcome-header", WidgetType.WELCOME_HEADER, "Welcome", 4, DataMode.LIVE, () ->
                WelcomeHeaderData.builder()
                        .displayName(user != null ? displayName(user) : "Super Admin")
                        .roleLabel(RoleLabels.of(com.thinkerscave.access.enums.RoleType.SUPER_ADMIN))
                        .organizationName("ThinkersCave Platform")
                        .greeting(RoleLabels.greeting())
                        .avatarUrl(user != null ? user.getProfileImageUrl() : null)
                        .todayLabel(java.time.LocalDate.now().toString())
                        .build());
    }

    private WidgetDTO<KpiGridData> kpiGrid() {
        return safeWidget("kpi-grid", WidgetType.KPI_GRID, "Platform overview", 4, DataMode.LIVE, () -> {
            long totalOrgs = organizationRepository.count();
            long activeOrgs = organizationRepository.countByStatus(OrganizationStatus.ACTIVE);
            long totalUsers = userRepository.count();
            long activeSubscriptions = organizationSubscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
            BigDecimal recurringRevenue = organizationSubscriptionRepository.sumActiveAnnualRevenue();

            return KpiGridData.builder().items(List.of(
                    KpiItem.builder().label("Organizations").value(String.valueOf(totalOrgs)).icon("pi-building").tone("primary").build(),
                    KpiItem.builder().label("Active Organizations").value(String.valueOf(activeOrgs)).icon("pi-check-circle").tone("success").build(),
                    KpiItem.builder().label("Total Users").value(String.valueOf(totalUsers)).icon("pi-users").tone("info").build(),
                    KpiItem.builder().label("Active Subscriptions").value(String.valueOf(activeSubscriptions)).icon("pi-credit-card").tone("warning").build(),
                    KpiItem.builder().label("Recurring Revenue").value(formatCurrency(recurringRevenue)).icon("pi-money-bill").tone("success").build(),
                    KpiItem.builder().label("Support Tickets").value("3").icon("pi-ticket").tone("danger").sample(true).build()
            )).build();
        });
    }

    private WidgetDTO<QuickActionsData> quickActions() {
        return safeWidget("quick-actions", WidgetType.QUICK_ACTIONS, "Quick actions", 4, DataMode.LIVE, () ->
                QuickActionsData.builder().items(List.of(
                        QuickActionItem.builder().label("Provision Organization").icon("pi-plus-circle").route("/app/tenant-management/provision-organization").tone("primary").build(),
                        QuickActionItem.builder().label("Manage Customers").icon("pi-users").route("/app/tenant-management/customers").tone("info").build(),
                        QuickActionItem.builder().label("Subscription Plans").icon("pi-credit-card").route("/app/tenant-management/subscription-plans").tone("success").build(),
                        QuickActionItem.builder().label("Provisioning Jobs").icon("pi-cog").route("/app/tenant-management/provisioning-jobs").tone("warning").build()
                )).build());
    }

    private WidgetDTO<ChartData> organizationGrowthChart() {
        return safeWidget("organization-growth", WidgetType.CHART, "Organization growth", "Cumulative organizations over the last 6 months",
                2, DataMode.LIVE, () -> {
            List<Organization> all = organizationRepository.findAll(PageRequest.of(0, 5000)).getContent();
            long baseline = all.size() - all.stream().filter(o -> o.getCreatedOn() != null
                    && o.getCreatedOn().isAfter(java.time.LocalDateTime.now().minusMonths(6))).count();
            List<java.time.LocalDateTime> timestamps = all.stream().map(Organization::getCreatedOn).collect(Collectors.toList());
            return ChartBucketUtil.cumulativeCounts(timestamps, Math.max(baseline, 0), 6, "Organizations", "line");
        });
    }

    private WidgetDTO<ChartData> newOrganizationsChart() {
        return safeWidget("new-organizations", WidgetType.CHART, "New organizations", "Per month, last 6 months",
                2, DataMode.LIVE, () -> {
            List<java.time.LocalDateTime> timestamps = organizationRepository.findAll(PageRequest.of(0, 5000)).stream()
                    .map(Organization::getCreatedOn).collect(Collectors.toList());
            return ChartBucketUtil.monthlyCounts(timestamps, 6, "New organizations", "bar");
        });
    }

    private WidgetDTO<ChartData> userGrowthChart() {
        return safeWidget("user-growth", WidgetType.CHART, "User growth", "Platform-wide, last 6 months",
                2, DataMode.LIVE, () -> {
            List<java.time.LocalDateTime> timestamps = userRepository.findAll(PageRequest.of(0, 5000)).stream()
                    .map(User::getCreatedOn).collect(Collectors.toList());
            return ChartBucketUtil.monthlyCounts(timestamps, 6, "New users", "area");
        });
    }

    private WidgetDTO<SystemHealthData> systemHealth() {
        return safeWidget("system-health", WidgetType.SYSTEM_HEALTH, "System health", 2, DataMode.SAMPLE, () ->
                SystemHealthData.builder()
                        .overallStatus("Operational")
                        .checks(List.of(
                                StatListItem.builder().label("API Gateway").value("Operational").icon("pi-check-circle").tone("success").build(),
                                StatListItem.builder().label("Database").value("Operational").icon("pi-check-circle").tone("success").build(),
                                StatListItem.builder().label("Background Jobs").value("Operational").icon("pi-check-circle").tone("success").build(),
                                StatListItem.builder().label("Email Delivery").value("Degraded").icon("pi-exclamation-triangle").tone("warning").build()
                        ))
                        .build());
    }

    private WidgetDTO<RecentRecordsData> recentOrganizations() {
        return safeWidget("recent-organizations", WidgetType.RECENT_RECORDS, "Recent organizations", 2, DataMode.LIVE, () -> {
            List<Organization> orgs = organizationRepository
                    .searchOrganizations(null, null, null, null, PageRequest.of(0, 5, Sort.by("createdOn").descending()))
                    .getContent();
            return RecentRecordsData.builder()
                    .columns(List.of("Organization", "Type", "Status"))
                    .items(orgs.stream().map(o -> RecordItem.builder()
                            .primaryLabel(o.getOrganizationName())
                            .secondaryLabel(o.getInstitutionType() != null ? o.getInstitutionType().name() : "-")
                            .statusLabel(o.getStatus() != null ? o.getStatus().name() : "-")
                            .statusTone(o.getStatus() == OrganizationStatus.ACTIVE ? "success" : "warning")
                            .timestampLabel(o.getCreatedOn() != null ? o.getCreatedOn().toLocalDate().toString() : "-")
                            .build()).collect(Collectors.toList()))
                    .build();
        });
    }

    private WidgetDTO<RecentActivityData> recentActivity() {
        return safeWidget("recent-activity", WidgetType.RECENT_ACTIVITY, "Recent activity", 2, DataMode.LIVE, () -> {
            List<AuditLog> logs = auditLogRepository.findAll(PageRequest.of(0, 6, Sort.by("occurredAt").descending())).getContent();
            return RecentActivityData.builder()
                    .items(logs.stream().map(l -> ActivityItem.builder()
                            .title(l.getAction())
                            .description(l.getSummary())
                            .actorName(l.getActorUsername())
                            .occurredAt(l.getOccurredAt())
                            .icon("pi-history")
                            .build()).collect(Collectors.toList()))
                    .build();
        });
    }

    private WidgetDTO<TopOrganizationsData> topOrganizations() {
        return safeWidget("top-organizations", WidgetType.TOP_ORGANIZATIONS, "Top organizations", "By active users", 2, DataMode.LIVE, () -> {
            List<Organization> orgs = organizationRepository.findAll(PageRequest.of(0, 5000)).getContent();
            List<TopOrgItem> items = orgs.stream()
                    .map(o -> new Object[]{o, userRepository.countByOrganizationIdAndStatus(o.getId(), UserStatus.ACTIVE)})
                    .sorted(Comparator.comparingLong((Object[] a) -> (Long) a[1]).reversed())
                    .limit(5)
                    .map(a -> {
                        Organization o = (Organization) a[0];
                        long activeUsers = (Long) a[1];
                        String planName = o.getOrganizationSubscription() != null
                                && o.getOrganizationSubscription().getSubscriptionPlan() != null
                                ? o.getOrganizationSubscription().getSubscriptionPlan().getPlanName() : "-";
                        return TopOrgItem.builder()
                                .organizationName(o.getOrganizationName())
                                .institutionType(o.getInstitutionType() != null ? o.getInstitutionType().name() : "-")
                                .activeUsers(activeUsers)
                                .planName(planName)
                                .build();
                    })
                    .collect(Collectors.toList());
            return TopOrganizationsData.builder().items(items).build();
        });
    }

    private WidgetDTO<SupportTicketsData> supportTicketsPreview() {
        return safeWidget("support-tickets-preview", WidgetType.SUPPORT_TICKETS, "Support tickets", "Future scope preview",
                2, DataMode.SAMPLE, sampleWidgetFactory::supportTickets);
    }

    private String displayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
        return (user.getFirstName() != null ? user.getFirstName() : "") +
                (user.getLastName() != null ? " " + user.getLastName() : "");
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹0";
        return "₹" + amount.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
