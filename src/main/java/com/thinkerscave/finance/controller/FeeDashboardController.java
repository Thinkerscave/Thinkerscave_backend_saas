package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.response.DashboardKpiResponse;
import com.thinkerscave.finance.dto.response.FeePaymentResponse;
import com.thinkerscave.finance.dto.response.FeeReceiptResponse;
import com.thinkerscave.finance.dto.response.OutstandingItemResponse;
import com.thinkerscave.finance.service.FeeDashboardService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.util.PageRequestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fees/dashboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FeeDashboardController {

    private final FeeDashboardService feeDashboardService;

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<DashboardKpiResponse>> kpis(@RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("KPIs", feeDashboardService.kpis(academicYearId)));
    }

    @GetMapping("/collection-trend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> collectionTrend(@RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Collection trend", feeDashboardService.collectionTrend(academicYearId)));
    }

    @GetMapping("/payment-status")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> paymentStatus(@RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Payment status", feeDashboardService.paymentStatus(academicYearId)));
    }

    @GetMapping("/upcoming-dues")
    public ResponseEntity<ApiResponse<List<OutstandingItemResponse>>> upcomingDues(@RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Upcoming dues", feeDashboardService.upcomingDues(academicYearId)));
    }

    @GetMapping("/recent-collections")
    public ResponseEntity<ApiResponse<PageResponse<FeePaymentResponse>>> recentCollections(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Recent collections",
                feeDashboardService.recentCollections(PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/overdue-students")
    public ResponseEntity<ApiResponse<PageResponse<OutstandingItemResponse>>> overdueStudents(
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Overdue students",
                feeDashboardService.overdueStudents(academicYearId, PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/upcoming-period-dues")
    public ResponseEntity<ApiResponse<PageResponse<OutstandingItemResponse>>> upcomingPeriodDues(
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Upcoming period dues",
                feeDashboardService.upcomingPeriodDues(academicYearId, PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/recent-receipts")
    public ResponseEntity<ApiResponse<PageResponse<FeeReceiptResponse>>> recentReceipts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Recent receipts",
                feeDashboardService.recentReceipts(PageRequestUtil.of(page, size, sort))));
    }
}
