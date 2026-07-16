package com.thinkerscave.dashboard.service.provider;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.dashboard.dto.response.WidgetDTO;

import java.util.List;

/**
 * One implementation per {@link com.thinkerscave.dashboard.enums.DashboardType}.
 * Implementations must never let a single widget's failure break the whole
 * response — build each widget defensively (see {@code AbstractDashboardWidgetProvider}).
 */
public interface DashboardWidgetProvider {

    List<WidgetDTO<?>> getWidgets(User user);
}
