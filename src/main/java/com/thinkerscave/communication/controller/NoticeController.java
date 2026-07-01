package com.thinkerscave.communication.controller;

import com.thinkerscave.communication.dto.request.NoticeRequest;
import com.thinkerscave.communication.dto.response.NoticeResponse;
import com.thinkerscave.communication.enums.NoticeStatus;
import com.thinkerscave.communication.service.NoticeService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/communication/notices")
@RequiredArgsConstructor
@Tag(name = "Communication - Notices", description = "Manage school notices and announcements")
@PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping
    @Operation(summary = "Create a notice")
    public ResponseEntity<ApiResponse<NoticeResponse>> create(@Valid @RequestBody NoticeRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Notice created", noticeService.create(request)));
    }

    @GetMapping
    @Operation(summary = "Get all notices (paged)")
    public ResponseEntity<ApiResponse<Page<NoticeResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Notices fetched", noticeService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notice by ID")
    public ResponseEntity<ApiResponse<NoticeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notice fetched", noticeService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a notice")
    public ResponseEntity<ApiResponse<NoticeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Notice updated", noticeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notice")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Notice deleted"));
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish a draft notice")
    public ResponseEntity<ApiResponse<NoticeResponse>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notice published", noticeService.publish(id)));
    }

    @GetMapping("/by-status")
    @Operation(summary = "Get notices by status (paged)")
    public ResponseEntity<ApiResponse<Page<NoticeResponse>>> getByStatus(
            @RequestParam NoticeStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Notices by status", noticeService.getByStatus(status, pageable)));
    }

    @GetMapping("/pinned")
    @Operation(summary = "Get pinned published notices")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getPinned() {
        return ResponseEntity.ok(ApiResponse.success("Pinned notices", noticeService.getPinnedPublished()));
    }
}
