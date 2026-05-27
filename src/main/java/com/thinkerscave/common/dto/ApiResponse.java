package com.thinkerscave.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * Canonical API response wrapper used by every REST controller.
 *
 * <p>Standard envelope:
 * <pre>{@code
 * {
 *   "success": true,
 *   "code": "OK",
 *   "message": "Student fetched successfully",
 *   "data": { ... },
 *   "meta": { "correlationId": "a1b2c3d4", "timestamp": "2026-05-27T10:30:00Z" }
 * }
 * }</pre>
 *
 * @param <T> payload type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private ApiMeta meta;

    // ─── Success Factories ────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return success("Request successful", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(ErrorCodes.OK)
                .message(message)
                .data(data)
                .meta(ApiMeta.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return created("Resource created successfully", data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(ErrorCodes.CREATED)
                .message(message)
                .data(data)
                .meta(ApiMeta.now())
                .build();
    }

    public static <T> ApiResponse<T> noContent(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(ErrorCodes.NO_CONTENT)
                .message(message)
                .meta(ApiMeta.now())
                .build();
    }

    // ─── Error Factories ──────────────────────────────────────────────────

    public static <T> ApiResponse<T> error(String message) {
        return error(ErrorCodes.INTERNAL_ERROR, message);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .meta(ApiMeta.now())
                .build();
    }

    // ─── Meta ─────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiMeta {
        private String correlationId;
        private Instant timestamp;

        public static ApiMeta now() {
            return ApiMeta.builder()
                    .correlationId(MDC.get("correlationId"))
                    .timestamp(Instant.now())
                    .build();
        }
    }
}
