package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.CreateReleaseRequest;
import com.thinkerscave.platform.dto.response.ReleaseResponse;
import com.thinkerscave.platform.dto.response.ReleaseSummaryResponse;
import com.thinkerscave.platform.service.ReleaseService;
import com.thinkerscave.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/releases")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class ReleaseOperationsController {
    private final ReleaseService releaseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReleaseResponse>> create(@Valid @RequestBody CreateReleaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Release created", releaseService.create(request)));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<ReleaseResponse>> execute(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Release execution completed", releaseService.execute(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReleaseResponse>>> history(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Release history retrieved", releaseService.history(pageable)));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<ReleaseResponse>> current() {
        return ResponseEntity.ok(ApiResponse.success("Current release retrieved", releaseService.current()));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReleaseSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Release summary retrieved", releaseService.summary()));
    }

}
