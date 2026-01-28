package com.thinkerscave.common.admission.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.thinkerscave.common.admission.dto.InquiryRequest;
import com.thinkerscave.common.admission.dto.InquiryResponse;
import com.thinkerscave.common.admission.service.InquiryService;
import com.thinkerscave.common.dto.ApiResponse;

import java.util.List;

import com.thinkerscave.common.admission.dto.FollowUpRequest;
import com.thinkerscave.common.admission.dto.FollowUpResponse;
import com.thinkerscave.common.admission.dto.InquirySummaryResponse;
import com.thinkerscave.common.admission.service.FollowUpService;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/staff/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final FollowUpService followUpService;

    @PostMapping
    public ResponseEntity<ApiResponse<InquiryResponse>> saveInquiry(
            @RequestBody InquiryRequest request) {

        InquiryResponse response = inquiryService.saveOrUpdate(request);

        return ResponseEntity.ok(
            ApiResponse.<InquiryResponse>builder()
                .success(true)
                .message("Inquiry saved successfully")
                .data(response)
                .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getAllInquiries() {

        return ResponseEntity.ok(
            ApiResponse.<List<InquiryResponse>>builder()
                .success(true)
                .message("Inquiry list fetched successfully")
                .data(inquiryService.getAll())
                .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInquiry(@PathVariable Long id) {

        inquiryService.delete(id);

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Inquiry deleted successfully")
                .build()
        );
    }

    // ---------------- Follow-Up Dashboard ----------------
    @GetMapping("/follow-ups")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getDashboardInquiries(
            @RequestParam(required = false, defaultValue = "UPCOMING") String tab,
            @RequestParam(required = false) Long counselorId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        List<InquiryResponse> list = inquiryService.getDashboardInquiries(tab, counselorId, fromDate, toDate);

        return ResponseEntity.ok(
            ApiResponse.<List<InquiryResponse>>builder()
                .success(true)
                .message("Fetched " + tab + " inquiries")
                .data(list)
                .build()
        );
    }


    // ---------------- Inquiry Summary ----------------
    @GetMapping("/{inquiryId}/summary")
    public ResponseEntity<ApiResponse<InquirySummaryResponse>> getInquirySummary(@PathVariable Long inquiryId) {
         InquirySummaryResponse summary = inquiryService.getInquirySummary(inquiryId);
         
         return ResponseEntity.ok(
            ApiResponse.<InquirySummaryResponse>builder()
                .success(true)
                .message("Inquiry summary fetched")
                .data(summary)
                .build()
        );
    }

    // ---------------- Follow-Up History ----------------
    @GetMapping("/{inquiryId}/follow-ups")
    public ResponseEntity<ApiResponse<List<FollowUpResponse>>> getFollowUps(@PathVariable Long inquiryId) {
        List<FollowUpResponse> list = followUpService.getFollowUps(inquiryId);

        return ResponseEntity.ok(
            ApiResponse.<List<FollowUpResponse>>builder()
                .success(true)
                .message("Follow-up history fetched")
                .data(list)
                .build()
        );
    }

    // ---------------- Add Follow-Up ----------------
    @PostMapping("/{inquiryId}/follow-ups")
    public ResponseEntity<ApiResponse<FollowUpResponse>> addFollowUp(
            @PathVariable Long inquiryId,
            @RequestBody FollowUpRequest request) {
        
        FollowUpResponse response = followUpService.addFollowUp(inquiryId, request);

         return ResponseEntity.ok(
            ApiResponse.<FollowUpResponse>builder()
                .success(true)
                .message("Follow-up added successfully")
                .data(response)
                .build()
        );
    }

    // ---------------- Proceed to Admission ----------------
    @PostMapping("/{inquiryId}/proceed-admission")
    public ResponseEntity<ApiResponse<Void>> proceedToAdmission(@PathVariable Long inquiryId) {
        inquiryService.proceedToAdmission(inquiryId);
        
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Inquiry marked ready for admission")
                .build()
        );
    }

    // ---------------- Mark Lost ----------------
    @PostMapping("/{inquiryId}/mark-lost")
    public ResponseEntity<ApiResponse<Void>> markLost(@PathVariable Long inquiryId) {
        inquiryService.markLost(inquiryId);

        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Inquiry marked as lost")
                .build()
        );
    }

}

