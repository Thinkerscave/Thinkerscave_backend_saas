package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.request.CloneFeeStructureRequest;
import com.thinkerscave.finance.dto.request.ConfigureClassFeeStructureRequest;
import com.thinkerscave.finance.dto.request.CopyClassFeeStructureRequest;
import com.thinkerscave.finance.dto.request.FeeStructureRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.ClassFeeStructureOverviewResponse;
import com.thinkerscave.finance.dto.response.ClonePreviewResponse;
import com.thinkerscave.finance.dto.response.FeeStructureResponse;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.security.FinanceAccessGuard;
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
    private final FinanceAccessGuard accessGuard;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeeStructureResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) FeeMasterStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Fee structures",
                feeStructureService.list(q, academicYearId, classId, status, PageRequestUtil.of(page, size, sort))));
    }

    /** Class-wise overview — every class Configured or Not Configured. */
    @GetMapping("/class-overview")
    public ResponseEntity<ApiResponse<PageResponse<ClassFeeStructureOverviewResponse>>> classOverview(
            @RequestParam Long academicYearId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String configuredStatus,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Class fee structures",
                feeStructureService.classOverview(
                        academicYearId, q, configuredStatus, PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/by-class")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> getByClass(
            @RequestParam Long academicYearId,
            @RequestParam Long classId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Class fee structure",
                feeStructureService.getByClass(academicYearId, classId)));
    }

    @PostMapping("/configure")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> configure(
            @Valid @RequestBody ConfigureClassFeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Fee structure configured",
                feeStructureService.configure(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> get(@PathVariable Long id) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Fee structure", feeStructureService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeeStructureResponse>> create(@Valid @RequestBody FeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.status(201).body(ApiResponse.created("Fee structure created", feeStructureService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody FeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Fee structure updated", feeStructureService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> status(@PathVariable Long id,
                                                                    @Valid @RequestBody StatusUpdateRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Fee structure status updated",
                feeStructureService.updateStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        feeStructureService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Fee structure deleted"));
    }

    @PostMapping("/{id}/clone/preview")
    public ResponseEntity<ApiResponse<ClonePreviewResponse>> clonePreview(
            @PathVariable Long id, @Valid @RequestBody CloneFeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Clone preview", feeStructureService.clonePreview(id, request)));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<List<FeeStructureResponse>>> clone(
            @PathVariable Long id, @Valid @RequestBody CloneFeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Structures cloned", feeStructureService.clone(id, request)));
    }

    @PostMapping("/{id}/copy-to-classes")
    public ResponseEntity<ApiResponse<List<FeeStructureResponse>>> copyToClasses(
            @PathVariable Long id, @Valid @RequestBody CopyClassFeeStructureRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_STRUCTURES);
        return ResponseEntity.ok(ApiResponse.success("Copied to classes",
                feeStructureService.copyToClasses(id, request)));
    }
}
