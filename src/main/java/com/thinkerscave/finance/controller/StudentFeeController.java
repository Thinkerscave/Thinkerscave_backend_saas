package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.response.*;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.service.StudentFeeService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.util.PageRequestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fees/students")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class StudentFeeController {

    private final StudentFeeService studentFeeService;
    private final FinanceAccessGuard accessGuard;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudentFeeListItemResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) BillingPeriodStatus status,
            @RequestParam(required = false) String periodKey,
            @RequestParam(required = false) Boolean outstandingOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        String effectiveSort = (sort == null || sort.isBlank()) ? "outstanding,desc" : sort;
        return ResponseEntity.ok(ApiResponse.success("Students",
                studentFeeService.list(q, academicYearId, classId, sectionId, status, periodKey, outstandingOnly,
                        PageRequestUtil.of(page, size, effectiveSort))));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<StudentFeeSummaryResponse>> summary(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) BillingPeriodStatus status,
            @RequestParam(required = false) String periodKey,
            @RequestParam(required = false) Boolean outstandingOnly) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Student fee summary",
                studentFeeService.summary(q, academicYearId, classId, sectionId, status, periodKey, outstandingOnly)));
    }

    @GetMapping("/billing-periods")
    public ResponseEntity<ApiResponse<List<BillingPeriodOptionResponse>>> billingPeriods(
            @RequestParam(required = false) Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Billing periods",
                studentFeeService.billingPeriodOptions(academicYearId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StudentFeeListItemResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Search", studentFeeService.search(q, academicYearId)));
    }

    @GetMapping("/linked")
    public ResponseEntity<ApiResponse<List<LinkedStudentResponse>>> linked() {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Linked students", studentFeeService.linked()));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentFeeDetailResponse>> detail(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Student fee detail",
                studentFeeService.detail(studentId, academicYearId)));
    }

    @GetMapping("/{studentId}/periods")
    public ResponseEntity<ApiResponse<List<BillingPeriodResponse>>> periods(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Periods", studentFeeService.periods(studentId, academicYearId)));
    }

    @GetMapping("/{studentId}/payments")
    public ResponseEntity<ApiResponse<List<FeePaymentResponse>>> payments(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Payments", studentFeeService.payments(studentId, academicYearId)));
    }

    @GetMapping("/{studentId}/receipts")
    public ResponseEntity<ApiResponse<List<FeeReceiptResponse>>> receipts(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Receipts", studentFeeService.receipts(studentId, academicYearId)));
    }

    @GetMapping("/{studentId}/academic-years")
    public ResponseEntity<ApiResponse<List<AcademicYearOptionResponse>>> years(@PathVariable Long studentId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Academic years", studentFeeService.academicYears(studentId)));
    }

    @GetMapping("/{studentId}/outstanding")
    public ResponseEntity<ApiResponse<StudentFeeKpiResponse>> outstanding(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_STUDENT_DETAILS);
        return ResponseEntity.ok(ApiResponse.success("Outstanding",
                studentFeeService.outstanding(studentId, academicYearId)));
    }
}
