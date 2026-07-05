package com.thinkerscave.security.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Resolved login scope from HTTP headers (platform vs tenant institution).
 */
@Getter
@Builder
public class LoginContext {

    public static final String HEADER = "X-Login-Context";
    public static final String PLATFORM = "PLATFORM";
    public static final String TENANT = "TENANT";
    public static final String PLATFORM_TENANT = "public";

    private final boolean platformLogin;
    private final String tenantIdentifier;
    private final Long organizationId;

    public static LoginContext fromHeaders(String loginContextHeader, String tenantHeader, String orgHeader) {
        String ctx = safe(loginContextHeader).toUpperCase();
        String tenant = normalizeTenantId(tenantHeader);
        Long orgId = parseLong(orgHeader);

        if (LoginContext.PLATFORM.equals(ctx) || LoginContext.PLATFORM_TENANT.equalsIgnoreCase(tenant)) {
            return LoginContext.builder()
                    .platformLogin(true)
                    .tenantIdentifier(LoginContext.PLATFORM_TENANT)
                    .organizationId(null)
                    .build();
        }

        return LoginContext.builder()
                .platformLogin(false)
                .tenantIdentifier(tenant)
                .organizationId(orgId)
                .build();
    }

    private static String normalizeTenantId(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return null;
        }
        return tenant.trim().toLowerCase();
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
