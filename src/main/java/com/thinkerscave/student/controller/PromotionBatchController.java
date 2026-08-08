package com.thinkerscave.student.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.student.dto.request.PromotionBatchCreateRequest;
import com.thinkerscave.student.dto.request.PromotionRecordUpdateRequest;
import com.thinkerscave.student.dto.response.PromotionBatchResponse;
import com.thinkerscave.student.dto.response.PromotionRecordResponse;
import com.thinkerscave.student.service.PromotionBatchService;
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
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Student Promotion Batches", description = "Grade promotion batch lifecycle (tenant-schema scoped)")
@PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','PRINCIPAL')")
public class PromotionBatchController {

    private final PromotionBatchService promotionBatchService;

    @GetMapping
    @Operation(summary = "List promotion batches")
    public ResponseEntity<ApiResponse<Page<PromotionBatchResponse>>> list(
            @PageableDefault(size = 20, sort = "batchId") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Promotion batches fetched",
                promotionBatchService.list(pageable)));
    }

    @PostMapping
    @Operation(summary = "Create a DRAFT promotion batch")
    public ResponseEntity<ApiResponse<PromotionBatchResponse>> create(
            @Valid @RequestBody PromotionBatchCreateRequest request) {
        return ResponseEntity.status(201).body(
                ApiResponse.created("Promotion batch created", promotionBatchService.create(request)));
    }

    @PostMapping("/{batchId}/preview")
    @Operation(summary = "Build eligible promotion records for a batch")
    public ResponseEntity<ApiResponse<List<PromotionRecordResponse>>> preview(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.success("Promotion preview ready",
                promotionBatchService.preview(batchId)));
    }

    @GetMapping("/{batchId}/records")
    @Operation(summary = "List promotion records for a batch")
    public ResponseEntity<ApiResponse<List<PromotionRecordResponse>>> records(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.success("Promotion records fetched",
                promotionBatchService.records(batchId)));
    }

    @PutMapping("/records/{recordId}")
    @Operation(summary = "Update a promotion record decision / target class")
    public ResponseEntity<ApiResponse<PromotionRecordResponse>> updateRecord(
            @PathVariable Long recordId,
            @RequestBody PromotionRecordUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Promotion record updated",
                promotionBatchService.updateRecord(recordId, request)));
    }

    @PostMapping("/{batchId}/execute")
    @Operation(summary = "Execute promotion batch (apply enrollments)")
    public ResponseEntity<ApiResponse<PromotionBatchResponse>> execute(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.success("Promotion batch executed",
                promotionBatchService.execute(batchId)));
    }

    @PostMapping("/{batchId}/rollback")
    @Operation(summary = "Rollback a completed promotion batch")
    public ResponseEntity<ApiResponse<PromotionBatchResponse>> rollback(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.success("Promotion batch rolled back",
                promotionBatchService.rollback(batchId)));
    }

    @PostMapping("/{batchId}/cancel")
    @Operation(summary = "Cancel a draft/in-progress promotion batch")
    public ResponseEntity<ApiResponse<PromotionBatchResponse>> cancel(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.success("Promotion batch cancelled",
                promotionBatchService.cancel(batchId)));
    }
}
