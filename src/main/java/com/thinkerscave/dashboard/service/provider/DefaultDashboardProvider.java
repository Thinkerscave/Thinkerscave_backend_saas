package com.thinkerscave.dashboard.service.provider;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.communication.entity.Notice;
import com.thinkerscave.communication.enums.NoticeStatus;
import com.thinkerscave.communication.repository.NoticeRepository;
import com.thinkerscave.dashboard.dto.response.WidgetDTO;
import com.thinkerscave.dashboard.dto.response.widgetdata.*;
import com.thinkerscave.dashboard.enums.DataMode;
import com.thinkerscave.dashboard.enums.WidgetType;
import com.thinkerscave.dashboard.util.RoleLabels;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fallback dashboard used whenever a user's role cannot be resolved to one
 * of the six dedicated dashboards, or a role's provider fails unexpectedly.
 * Deliberately minimal and always safe to render — no assumptions about
 * organization membership beyond what {@link OrganizationContext} provides.
 */
@Component
@RequiredArgsConstructor
public class DefaultDashboardProvider extends AbstractDashboardWidgetProvider implements DashboardWidgetProvider {

    private final NoticeRepository noticeRepository;

    @Override
    public List<WidgetDTO<?>> getWidgets(User user) {
        return List.of(
                welcomeHeader(user),
                profileSummary(user),
                notifications(),
                helpAndSupport(),
                recentAnnouncements()
        );
    }

    private WidgetDTO<WelcomeHeaderData> welcomeHeader(User user) {
        return safeWidget("welcome-header", WidgetType.WELCOME_HEADER, "Welcome", 4, DataMode.LIVE, () ->
                WelcomeHeaderData.builder()
                        .displayName(user != null ? displayName(user) : "there")
                        .roleLabel(RoleLabels.of(null))
                        .greeting(RoleLabels.greeting())
                        .avatarUrl(user != null ? user.getProfileImageUrl() : null)
                        .todayLabel(LocalDate.now().toString())
                        .build());
    }

    private WidgetDTO<ProfileSummaryData> profileSummary(User user) {
        return safeWidget("profile-summary", WidgetType.PROFILE_SUMMARY, "Your profile", 2, DataMode.LIVE, () ->
                ProfileSummaryData.builder()
                        .displayName(user != null ? displayName(user) : "-")
                        .email(user != null ? user.getEmail() : null)
                        .mobileNumber(user != null ? user.getMobileNumber() : null)
                        .roleLabel(RoleLabels.of(null))
                        .avatarUrl(user != null ? user.getProfileImageUrl() : null)
                        .build());
    }

    private WidgetDTO<NotificationsData> notifications() {
        return safeWidget("notifications", WidgetType.NOTIFICATIONS, "Notifications", 2, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            if (orgId == null) return NotificationsData.builder().items(List.of()).unreadCount(0).build();
            List<Notice> notices = noticeRepository.findByOrganizationIdAndStatusOrderByPublishDateDesc(
                    orgId, NoticeStatus.PUBLISHED, PageRequest.of(0, 5)).getContent();
            return NotificationsData.builder()
                    .unreadCount(notices.size())
                    .items(notices.stream().map(n -> NotificationItem.builder()
                            .title(n.getTitle()).message(trim(n.getContent())).date(n.getPublishDate())
                            .category(n.getCategory()).pinned(n.isPinned()).build()).collect(Collectors.toList()))
                    .build();
        });
    }

    private WidgetDTO<StatListData> helpAndSupport() {
        return safeWidget("help-support", WidgetType.STAT_LIST, "Help & support", 2, DataMode.LIVE, () ->
                StatListData.builder().items(List.of(
                        StatListItem.builder().label("Support Email").value("support@thinkerscave.com").icon("pi-envelope").tone("info").build(),
                        StatListItem.builder().label("Contact Administrator").value("Reach your organization admin").icon("pi-user").tone("primary").build(),
                        StatListItem.builder().label("Documentation").value("Help Center").icon("pi-book").tone("success").build()
                )).build());
    }

    private WidgetDTO<AnnouncementsData> recentAnnouncements() {
        return safeWidget("recent-announcements", WidgetType.ANNOUNCEMENTS, "Recent announcements", 4, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            if (orgId == null) return AnnouncementsData.builder().items(List.of()).build();
            List<Notice> notices = noticeRepository.findByOrganizationIdAndStatusOrderByPublishDateDesc(
                    orgId, NoticeStatus.PUBLISHED, PageRequest.of(0, 5)).getContent();
            return AnnouncementsData.builder()
                    .items(notices.stream().map(n -> AnnouncementItem.builder()
                            .title(n.getTitle()).summary(trim(n.getContent())).category(n.getCategory())
                            .publishedAt(n.getPublishDate()).pinned(n.isPinned()).build()).collect(Collectors.toList()))
                    .build();
        });
    }

    private String displayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
        return (user.getFirstName() != null ? user.getFirstName() : "") +
                (user.getLastName() != null ? " " + user.getLastName() : "");
    }

    private String trim(String content) {
        if (content == null) return null;
        return content.length() > 140 ? content.substring(0, 140) + "…" : content;
    }
}
