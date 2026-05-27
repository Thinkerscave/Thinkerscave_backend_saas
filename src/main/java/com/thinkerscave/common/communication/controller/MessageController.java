package com.thinkerscave.common.communication.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.communication.dto.MessageDTO;
import com.thinkerscave.common.communication.dto.MessageThreadDTO;
import com.thinkerscave.common.communication.service.MessageService;
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

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Messages", description = "Direct-message threads & messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List message threads")
    public ResponseEntity<ApiResponse<PageResponse<MessageThreadDTO>>> listThreads(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                messageService.listThreads(PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/threads/{threadId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageThreadDTO>> getThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(ApiResponse.success(messageService.getThread(threadId)));
    }

    @PostMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Open a new message thread")
    public ResponseEntity<ApiResponse<MessageThreadDTO>> createThread(@Valid @RequestBody MessageThreadDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Thread opened", messageService.createThread(dto)));
    }

    @PostMapping("/threads/{threadId}/close")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageThreadDTO>> closeThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(ApiResponse.success("Thread closed", messageService.closeThread(threadId)));
    }

    @GetMapping("/threads/{threadId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<MessageDTO>>> list(
            @PathVariable Long threadId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                messageService.listMessages(threadId, PageRequestUtil.of(page, size, null)))));
    }

    @PostMapping("/threads/{threadId}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Post a message to a thread")
    public ResponseEntity<ApiResponse<MessageDTO>> post(@PathVariable Long threadId,
                                                        @Valid @RequestBody MessageDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Message posted",
                messageService.postMessage(threadId, dto)));
    }

    @DeleteMapping("/{messageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Soft-delete a message")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long messageId) {
        messageService.softDeleteMessage(messageId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted", null));
    }
}
