package com.thinkerscave.platform.dto.response;

/**
 * One validation rule outcome. status is PASS, WARN or FAIL.
 */
public record ValidationCheckResponse(
        String key,
        String label,
        String status,
        String message
) {
}
