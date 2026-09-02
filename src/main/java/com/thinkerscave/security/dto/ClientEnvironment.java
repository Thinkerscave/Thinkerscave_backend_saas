package com.thinkerscave.security.dto;

import org.springframework.util.StringUtils;

/**
 * Client details captured at login/logout so login history can show IP and device.
 */
public record ClientEnvironment(
        String ipAddress,
        String userAgent,
        String deviceName,
        String browser,
        String operatingSystem
) {

    public static ClientEnvironment empty() {
        return new ClientEnvironment(null, null, null, null, null);
    }

    public static ClientEnvironment of(String ipAddress, String userAgent, String deviceName) {
        String ua = StringUtils.hasText(userAgent) ? userAgent.trim() : null;
        return new ClientEnvironment(
                blankToNull(ipAddress),
                ua,
                blankToNull(deviceName),
                parseBrowser(ua),
                parseOperatingSystem(ua)
        );
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    static String parseBrowser(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) {
            return "Edge";
        }
        if (ua.contains("opr/") || ua.contains("opera")) {
            return "Opera";
        }
        if (ua.contains("chrome/") && !ua.contains("chromium")) {
            return "Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome")) {
            return "Safari";
        }
        return "Browser";
    }

    static String parseOperatingSystem(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("mac os") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return null;
    }
}
