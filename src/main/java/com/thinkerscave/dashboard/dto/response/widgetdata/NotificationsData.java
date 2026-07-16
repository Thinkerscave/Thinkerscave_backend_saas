package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationsData {
    private List<NotificationItem> items;
    private int unreadCount;
}
