package com.thinkerscave.shared.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the acting principal for audit attribution.
 *
 * <p>Platform operations (releases, tenant migrations, catalog sync, maintenance)
 * must record the operator who triggered them. Background/scheduled work has no
 * request context, so {@link #SYSTEM} is used as the fallback.</p>
 */
public final class CurrentActor {

    public static final String SYSTEM = "system";

    private CurrentActor() {
    }

    public static String resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return SYSTEM;
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? SYSTEM : name;
    }
}
