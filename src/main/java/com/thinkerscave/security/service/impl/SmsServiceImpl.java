package com.thinkerscave.security.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.security.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * SMS delivery with zero-cost defaults.
 * <ul>
 *   <li>{@code log} — writes the SMS to server logs (always free; default)</li>
 *   <li>{@code textbelt} — Textbelt.com free key ({@code textbelt}) = 1 SMS/day, or your paid key</li>
 * </ul>
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    private static final Pattern NON_DIGIT = Pattern.compile("\\D+");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.notification.sms.enabled:true}")
    private boolean enabled;

    @Value("${app.notification.sms.provider:log}")
    private String provider;

    @Value("${app.notification.sms.api-key:textbelt}")
    private String apiKey;

    @Value("${app.notification.sms.default-country-code:91}")
    private String defaultCountryCode;

    @Value("${app.notification.sms.sender:ThinkersCave}")
    private String sender;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean sendSms(String mobileNumber, String message) {
        if (!enabled) {
            log.debug("SMS skipped (disabled)");
            return false;
        }
        if (!StringUtils.hasText(mobileNumber) || !StringUtils.hasText(message)) {
            log.warn("SMS skipped: missing mobile or message");
            return false;
        }

        String normalized = normalizePhone(mobileNumber);
        if (!StringUtils.hasText(normalized)) {
            log.warn("SMS skipped: invalid mobile '{}'", mobileNumber);
            return false;
        }

        String mode = provider == null ? "log" : provider.trim().toLowerCase();
        return switch (mode) {
            case "textbelt" -> sendViaTextbelt(normalized, message);
            case "log" -> {
                log.warn("SMS[log] to={} message={}", normalized, message);
                yield true;
            }
            default -> {
                log.warn("Unknown SMS provider '{}'; falling back to log. to={} message={}",
                        provider, normalized, message);
                log.warn("SMS[log-fallback] to={} message={}", normalized, message);
                yield true;
            }
        };
    }

    private boolean sendViaTextbelt(String phoneE164Digits, String message) {
        try {
            String key = StringUtils.hasText(apiKey) ? apiKey.trim() : "textbelt";
            String form = "phone=" + URLEncoder.encode(phoneE164Digits, StandardCharsets.UTF_8)
                    + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
                    + "&key=" + URLEncoder.encode(key, StandardCharsets.UTF_8)
                    + "&sender=" + URLEncoder.encode(sender, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://textbelt.com/text"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            boolean success = body.path("success").asBoolean(false);
            if (success) {
                log.info("SMS[textbelt] sent to={} quotaRemaining={}",
                        phoneE164Digits, body.path("quotaRemaining").asText("?"));
                return true;
            }
            log.error("SMS[textbelt] failed to={} error={} body={}",
                    phoneE164Digits, body.path("error").asText("unknown"), response.body());
            // Always keep a free fallback so OTP/onboarding is still usable
            log.warn("SMS[log-fallback] to={} message={}", phoneE164Digits, message);
            return false;
        } catch (Exception ex) {
            log.error("SMS[textbelt] exception to={}: {}", phoneE164Digits, ex.getMessage());
            log.warn("SMS[log-fallback] to={} message={}", phoneE164Digits, message);
            return false;
        }
    }

    /**
     * Normalizes to digits suitable for Textbelt (E.164 without '+').
     * Indian 10-digit numbers get country code 91 by default.
     */
    private String normalizePhone(String raw) {
        String digits = NON_DIGIT.matcher(raw.trim()).replaceAll("");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() == 10 && StringUtils.hasText(defaultCountryCode)) {
            return defaultCountryCode + digits;
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            return defaultCountryCode + digits.substring(1);
        }
        return digits;
    }
}
