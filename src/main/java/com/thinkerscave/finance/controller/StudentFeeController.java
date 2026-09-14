package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.response.*;
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

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudentFeeListItemResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Students",
                studentFeeService.list(q, academicYearId, classId, sectionId, PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StudentFeeListItemResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Search", studentFeeService.search(q, academicYearId)));
    }

    @GetMapping("/linked")
    public ResponseEntity<ApiResponse<List<LinkedStudentResponse>>> linked() {
        return ResponseEntity.ok(ApiResponse.success("Linked students", studentFeeService.linked()));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentFeeDetailResponse>> detail(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Student fee detail",
                studentFeeService.detail(studentId, academicYearId)));
    }

    @GetMapping("/{studentId}/periods")
    public ResponseEntity<ApiResponse<List<BillingPeriodResponse>>> periods(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Periods", studentFeeService.periods(studentId, academicYearId)));
    }

    @GetMapping("/{studentId}/payments")
    public ResponseEntity<ApiResponse<List<FeePaymentResponse>>> payments(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Payments", studentFeeService.payments(studentId, academicYearId)));
    }

    @GetMapping("/{studentId}/receipts")
    public ResponseEntity<ApiResponse<List<FeeReceiptResponse>>> receipts(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Receipts", studentFeeService.receipts(studentId, academicYearId)));
    }

    @GetMapping("/{studentId}/academic-years")
    public ResponseEntity<ApiResponse<List<AcademicYearOptionResponse>>> years(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Academic years", studentFeeService.academicYears(studentId)));
    }

    @GetMapping("/{studentId}/outstanding")
    public ResponseEntity<ApiResponse<StudentFeeKpiResponse>> outstanding(
            @PathVariable Long studentId, @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Outstanding",
                studentFeeService.outstanding(studentId, academicYearId)));
    }
}
