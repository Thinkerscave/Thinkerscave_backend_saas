package com.thinkerscave.dashboard.service.provider;

import com.thinkerscave.dashboard.dto.response.WidgetDTO;
import com.thinkerscave.dashboard.enums.DataMode;
import com.thinkerscave.dashboard.enums.WidgetState;
import com.thinkerscave.dashboard.enums.WidgetType;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Shared helper for building widgets defensively: any exception thrown
 * while assembling a single widget's data is caught and converted into an
 * {@code ERROR} state widget instead of failing the entire dashboard
 * response.
 */
@Slf4j
public abstract class AbstractDashboardWidgetProvider {

    protected <T> WidgetDTO<T> safeWidget(String widgetKey, WidgetType type, String title, Integer span,
                                            DataMode dataMode, Supplier<T> dataSupplier) {
        return safeWidget(widgetKey, type, title, null, span, dataMode, dataSupplier);
    }

    protected <T> WidgetDTO<T> safeWidget(String widgetKey, WidgetType type, String title, String subtitle,
                                            Integer span, DataMode dataMode, Supplier<T> dataSupplier) {
        try {
            T data = dataSupplier.get();
            boolean empty = data == null || hasNoContent(data);
            return WidgetDTO.<T>builder()
                    .widgetKey(widgetKey)
                    .widgetType(type)
                    .title(title)
                    .subtitle(subtitle)
                    .span(span)
                    .dataMode(dataMode)
                    .state(empty ? WidgetState.EMPTY : WidgetState.SUCCESS)
                    .data(data)
                    .build();
        } catch (Exception e) {
            log.warn("Widget '{}' ({}) failed to build: {}", widgetKey, type, e.getMessage(), e);
            return WidgetDTO.<T>builder()
                    .widgetKey(widgetKey)
                    .widgetType(type)
                    .title(title)
                    .subtitle(subtitle)
                    .span(span)
                    .dataMode(dataMode)
                    .state(WidgetState.ERROR)
                    .errorMessage("Unable to load this widget right now.")
                    .build();
        }
    }

    /**
     * Most widget payloads are shaped around one or more list/collection fields
     * (e.g. {@code items}, {@code checks}, {@code slots}, {@code children}).
     * A widget whose only meaningful content is such a collection, but that
     * collection came back empty, should render the card's EMPTY state rather
     * than a blank body. Scalar-only DTOs (welcome header, fee summary, etc.)
     * have no collection getters and are therefore never affected by this check.
     */
    private boolean hasNoContent(Object data) {
        boolean foundCollection = false;
        for (Method method : data.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || !Modifier.isPublic(method.getModifiers())) continue;
            String name = method.getName();
            boolean isGetter = (name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2);
            if (!isGetter || name.equals("getClass")) continue;
            Class<?> returnType = method.getReturnType();
            if (!Collection.class.isAssignableFrom(returnType) && !Map.class.isAssignableFrom(returnType)) continue;
            try {
                Object value = method.invoke(data);
                foundCollection = true;
                boolean valueEmpty = value == null
                        || (value instanceof Collection && ((Collection<?>) value).isEmpty())
                        || (value instanceof Map && ((Map<?, ?>) value).isEmpty());
                if (!valueEmpty) {
                    return false;
                }
            } catch (ReflectiveOperationException ignored) {
                // Defensive: if we can't introspect, don't let it affect the state.
            }
        }
        return foundCollection;
    }
}
