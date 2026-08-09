package com.thinkerscave.communication.controller;

import com.thinkerscave.communication.dto.request.NotificationRequest;
import com.thinkerscave.communication.dto.response.NotificationResponse;
import com.thinkerscave.communication.enums.NotificationStatus;
import com.thinkerscave.communication.service.NotificationService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/communication/notifications")
@RequiredArgsConstructor
@Tag(name = "Communication - Notifications", description = "Send and manage notifications")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Send a notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Notification queued", notificationService.send(request)));
    }

    @GetMapping
    @Operation(summary = "Get all notifications (paged)")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", notificationService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification fetched", notificationService.getById(id)));
    }

    @GetMapping("/by-status")
    @Operation(summary = "Get notifications by status (paged)")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getByStatus(
            @RequestParam NotificationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Notifications by status", notificationService.getByStatus(status, pageable)));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending/queued notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification cancelled", notificationService.cancel(id)));
    }
}
