package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.request.FeeHeadRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.FeeHeadResponse;
import com.thinkerscave.finance.enums.FeeHeadCategory;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.service.FeeHeadService;
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
@RequestMapping("/api/v1/fees/heads")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FeeHeadController {

    private final FeeHeadService feeHeadService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeeHeadResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) FeeHeadCategory category,
            @RequestParam(required = false) FeeMasterStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Fee heads",
                feeHeadService.list(q, category, status, PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/lookups")
    public ResponseEntity<ApiResponse<List<FeeHeadResponse>>> lookups() {
        return ResponseEntity.ok(ApiResponse.success("Fee head lookups", feeHeadService.lookups()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeHeadResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Fee head", feeHeadService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeeHeadResponse>> create(@Valid @RequestBody FeeHeadRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created("Fee head created", feeHeadService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeHeadResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody FeeHeadRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fee head updated", feeHeadService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FeeHeadResponse>> status(@PathVariable Long id,
                                                               @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fee head status updated", feeHeadService.updateStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feeHeadService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Fee head deleted"));
    }
}
