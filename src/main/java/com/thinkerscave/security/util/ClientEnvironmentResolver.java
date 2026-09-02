package com.thinkerscave.security.util;

import com.thinkerscave.security.dto.ClientEnvironment;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientEnvironmentResolver {

    private ClientEnvironmentResolver() {}

    public static ClientEnvironment from(HttpServletRequest request, String deviceName) {
        if (request == null) {
            return ClientEnvironment.of(null, null, deviceName);
        }
        return ClientEnvironment.of(clientIp(request), request.getHeader("User-Agent"), deviceName);
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
