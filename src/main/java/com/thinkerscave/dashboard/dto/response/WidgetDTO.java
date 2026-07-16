package com.thinkerscave.dashboard.dto.response;

import com.thinkerscave.dashboard.enums.DataMode;
import com.thinkerscave.dashboard.enums.WidgetState;
import com.thinkerscave.dashboard.enums.WidgetType;
import lombok.Builder;
import lombok.Data;

/**
 * Generic envelope for a single dashboard widget. The frontend maps
 * {@link #widgetType} to a component via a widget registry and renders
 * {@link #data} accordingly; it never needs to know which role/dashboard
 * produced it.
 *
 * @param <T> the widget-specific payload type (see {@code widgetdata} package)
 */
@Data
@Builder
public class WidgetDTO<T> {

    /** Stable identifier for this widget instance, e.g. "kpi-grid", "recent-activity". */
    private String widgetKey;

    private WidgetType widgetType;

    private String title;

    private String subtitle;

    /** Layout hint: how many of the 4 desktop grid columns this widget should span (1-4). */
    private Integer span;

    private DataMode dataMode;

    @Builder.Default
    private WidgetState state = WidgetState.SUCCESS;

    /** Populated only when {@link #state} is {@code ERROR}. */
    private String errorMessage;

    private T data;
}
