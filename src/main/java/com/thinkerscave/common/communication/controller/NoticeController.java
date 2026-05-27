package com.thinkerscave.common.communication.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.communication.domain.NoticeStatus;
import com.thinkerscave.common.communication.dto.NoticeDTO;
import com.thinkerscave.common.communication.service.NoticeService;
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

@RestController
@RequestMapping("/api/v1/notices")
@Tag(name = "Notices", description = "Notice / announcement workflow")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('NOTICE_VIEW')")
    @Operation(summary = "List notices by status")
    public ResponseEntity<ApiResponse<PageResponse<NoticeDTO>>> list(
            @RequestParam(defaultValue = "PUBLISHED") NoticeStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                noticeService.listByStatus(status, PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "All notices visible today")
    public ResponseEntity<ApiResponse<List<NoticeDTO>>> active() {
        return ResponseEntity.ok(ApiResponse.success(noticeService.activeForToday()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NoticeDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('NOTICE_EDIT')")
    public ResponseEntity<ApiResponse<NoticeDTO>> save(@Valid @RequestBody NoticeDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Notice created" : "Notice updated", noticeService.save(dto)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('NOTICE_PUBLISH')")
    public ResponseEntity<ApiResponse<NoticeDTO>> publish(@PathVariable Long id,
                                                          @RequestParam(required = false) Long byUserId) {
        return ResponseEntity.ok(ApiResponse.success("Notice published", noticeService.publish(id, byUserId)));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('NOTICE_EDIT')")
    public ResponseEntity<ApiResponse<NoticeDTO>> archive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notice archived", noticeService.archive(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('NOTICE_EDIT')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Notice deleted", null));
    }
}
