package com.thinkerscave.common.communication.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.communication.domain.NotificationStatus;
import com.thinkerscave.common.communication.dto.NotificationDTO;
import com.thinkerscave.common.communication.dto.NotificationRecipientDTO;
import com.thinkerscave.common.communication.service.NotificationService;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "System & user notifications dispatch + inbox")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "List notifications (admin)")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDTO>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                notificationService.list(PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Get notification with recipient breakdown")
    public ResponseEntity<ApiResponse<NotificationDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.get(id)));
    }

    @GetMapping("/{id}/recipients")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "List recipient delivery rows for a notification")
    public ResponseEntity<ApiResponse<List<NotificationRecipientDTO>>> recipients(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.recipientsOf(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('NOTIFICATION_SEND')")
    @Operation(summary = "Create & dispatch a notification (recipients in request body)")
    public ResponseEntity<ApiResponse<NotificationDTO>> create(
            @Valid @RequestBody NotificationCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.created("Notification created",
                notificationService.create(req.notification, req.recipients)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('NOTIFICATION_SEND')")
    @Operation(summary = "Cancel a pending/queued notification")
    public ResponseEntity<ApiResponse<NotificationDTO>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification cancelled",
                notificationService.cancel(id)));
    }

    // ---- recipient state callbacks (provider webhooks / client read receipts) ----

    @PostMapping("/recipients/{recipientId}/sent")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','SYSTEM') or hasAuthority('NOTIFICATION_DISPATCH')")
    @Operation(summary = "Mark a recipient row as SENT (provider webhook)")
    public ResponseEntity<ApiResponse<NotificationRecipientDTO>> markSent(
            @PathVariable Long recipientId,
            @RequestParam(required = false) String providerMessageId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markSent(recipientId, providerMessageId)));
    }

    @PostMapping("/recipients/{recipientId}/delivered")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','SYSTEM') or hasAuthority('NOTIFICATION_DISPATCH')")
    public ResponseEntity<ApiResponse<NotificationRecipientDTO>> markDelivered(@PathVariable Long recipientId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markDelivered(recipientId)));
    }

    @PostMapping("/recipients/{recipientId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationRecipientDTO>> markRead(@PathVariable Long recipientId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markRead(recipientId)));
    }

    @PostMapping("/recipients/{recipientId}/failed")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','SYSTEM') or hasAuthority('NOTIFICATION_DISPATCH')")
    public ResponseEntity<ApiResponse<NotificationRecipientDTO>> markFailed(
            @PathVariable Long recipientId, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markFailed(recipientId, reason)));
    }

    // ---- per-user inbox ----

    @GetMapping("/inbox/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List a user's notification inbox")
    public ResponseEntity<ApiResponse<PageResponse<NotificationRecipientDTO>>> inbox(
            @PathVariable Long userId,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                notificationService.inbox(userId, status, PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/inbox/{userId}/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Count of unread notifications for a user")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unread(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("unread", notificationService.unreadCount(userId))));
    }

    /** Request body wrapper for create. */
    public static class NotificationCreateRequest {
        @Valid public NotificationDTO notification;
        @Valid public List<NotificationRecipientDTO> recipients;
    }
}
