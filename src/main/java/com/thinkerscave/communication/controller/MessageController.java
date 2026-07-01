package com.thinkerscave.communication.controller;

import com.thinkerscave.communication.dto.request.MessageRequest;
import com.thinkerscave.communication.dto.request.MessageThreadRequest;
import com.thinkerscave.communication.dto.response.MessageResponse;
import com.thinkerscave.communication.dto.response.MessageThreadResponse;
import com.thinkerscave.communication.service.MessageService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/communication/messages")
@RequiredArgsConstructor
@Tag(name = "Communication - Messages", description = "Internal messaging between users")
@PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF', 'STUDENT', 'PARENT')")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/threads")
    @Operation(summary = "Start a new message thread")
    public ResponseEntity<ApiResponse<MessageThreadResponse>> createThread(
            @Valid @RequestBody MessageThreadRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Thread created", messageService.createThread(request)));
    }

    @GetMapping("/threads")
    @Operation(summary = "Get my message threads")
    public ResponseEntity<ApiResponse<Page<MessageThreadResponse>>> getMyThreads(
            @RequestParam Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Threads fetched", messageService.getMyThreads(userId, pageable)));
    }

    @PostMapping("/threads/{threadId}")
    @Operation(summary = "Send a message in a thread")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable Long threadId,
            @RequestParam Long senderUserId,
            @Valid @RequestBody MessageRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Message sent",
                messageService.sendMessage(threadId, request, senderUserId)));
    }

    @GetMapping("/threads/{threadId}")
    @Operation(summary = "Get messages in a thread (paged)")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
            @PathVariable Long threadId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Messages fetched", messageService.getMessages(threadId, pageable)));
    }

    @PutMapping("/threads/{threadId}/close")
    @Operation(summary = "Close a message thread")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> closeThread(@PathVariable Long threadId) {
        messageService.closeThread(threadId);
        return ResponseEntity.ok(ApiResponse.noContent("Thread closed"));
    }
}
