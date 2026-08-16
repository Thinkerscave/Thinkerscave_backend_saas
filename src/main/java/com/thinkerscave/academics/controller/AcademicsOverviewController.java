package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.response.AcademicsOverviewResponse;
import com.thinkerscave.academics.service.AcademicsOverviewService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
@Tag(name = "Academics Overview", description = "Academics admin dashboard overview")
public class AcademicsOverviewController {

    private final AcademicsOverviewService overviewService;

    @GetMapping("/years/{yearId}/overview")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get academics overview dashboard for an academic year")
    public ResponseEntity<ApiResponse<AcademicsOverviewResponse>> overview(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Academics overview", overviewService.getOverview(yearId)));
    }
}
