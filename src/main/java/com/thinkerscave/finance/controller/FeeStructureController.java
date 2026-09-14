package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.request.CloneFeeStructureRequest;
import com.thinkerscave.finance.dto.request.FeeStructureRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.ClonePreviewResponse;
import com.thinkerscave.finance.dto.response.FeeStructureResponse;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.service.FeeStructureService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.util.PageRequestUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fees/structures")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeeStructureResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) FeeMasterStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Fee structures",
                feeStructureService.list(q, academicYearId, classId, status, PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Fee structure", feeStructureService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeeStructureResponse>> create(@Valid @RequestBody FeeStructureRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created("Fee structure created", feeStructureService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody FeeStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fee structure updated", feeStructureService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> status(@PathVariable Long id,
                                                                    @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fee structure status updated",
                feeStructureService.updateStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feeStructureService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Fee structure deleted"));
    }

    @PostMapping("/{id}/clone/preview")
    public ResponseEntity<ApiResponse<ClonePreviewResponse>> clonePreview(
            @PathVariable Long id, @Valid @RequestBody CloneFeeStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Clone preview", feeStructureService.clonePreview(id, request)));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<List<FeeStructureResponse>>> clone(
            @PathVariable Long id, @Valid @RequestBody CloneFeeStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Structures cloned", feeStructureService.clone(id, request)));
    }
}
