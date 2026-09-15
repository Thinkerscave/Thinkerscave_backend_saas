package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.response.OutstandingItemResponse;
import com.thinkerscave.finance.dto.response.OutstandingSummaryResponse;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.service.FeeOutstandingService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.util.PageRequestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fees/outstanding")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OutstandingController {

    private final FeeOutstandingService feeOutstandingService;
    private final FinanceAccessGuard accessGuard;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OutstandingItemResponse>>> list(
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) BillingPeriodStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
            accessGuard.requireView(FinanceAccessGuard.RESOURCE_OUTSTANDING);
        return ResponseEntity.ok(ApiResponse.success("Outstanding",
                feeOutstandingService.list(academicYearId, classId, sectionId, status,
                        PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<OutstandingSummaryResponse>> summary(@RequestParam Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_OUTSTANDING);
        return ResponseEntity.ok(ApiResponse.success("Outstanding summary",
                feeOutstandingService.summary(academicYearId)));
    }
}
