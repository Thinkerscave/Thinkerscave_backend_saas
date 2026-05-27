package com.thinkerscave.common.promotion.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import com.thinkerscave.common.promotion.dto.PromotionBatchDTO;
import com.thinkerscave.common.promotion.dto.PromotionRecordDTO;
import com.thinkerscave.common.promotion.service.PromotionService;
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
@RequestMapping("/api/v1/promotions")
@Tag(name = "Promotions", description = "Year-end mass promotion workflow")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<PromotionBatchDTO>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                promotionService.list(PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<PromotionBatchDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.get(id)));
    }

    @GetMapping("/{id}/records")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<List<PromotionRecordDTO>>> records(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.records(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('PROMOTION_EDIT')")
    public ResponseEntity<ApiResponse<PromotionBatchDTO>> create(
            @Valid @RequestBody PromotionBatchDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Promotion batch created",
                promotionService.createBatch(dto)));
    }

    @PostMapping("/{id}/preview")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('PROMOTION_EDIT')")
    @Operation(summary = "Generate candidate records from active enrollments in source class")
    public ResponseEntity<ApiResponse<List<PromotionRecordDTO>>> preview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Preview generated",
                promotionService.preview(id)));
    }

    @PutMapping("/records/{recordId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('PROMOTION_EDIT')")
    public ResponseEntity<ApiResponse<PromotionRecordDTO>> updateRecord(
            @PathVariable Long recordId, @Valid @RequestBody PromotionRecordDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Record updated",
                promotionService.updateRecord(recordId, dto)));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('PROMOTION_APPROVE')")
    public ResponseEntity<ApiResponse<PromotionBatchDTO>> execute(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Promotion executed",
                promotionService.execute(id)));
    }

    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('PROMOTION_APPROVE')")
    public ResponseEntity<ApiResponse<PromotionBatchDTO>> rollback(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Promotion rolled back",
                promotionService.rollback(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('PROMOTION_EDIT')")
    public ResponseEntity<ApiResponse<PromotionBatchDTO>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Promotion cancelled",
                promotionService.cancel(id)));
    }
}
