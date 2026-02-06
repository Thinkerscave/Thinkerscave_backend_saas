package com.thinkerscave.common.admission.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class InquiryController {

        private final InquiryService inquiryService;
        private final FollowUpService followUpService;

        @io.swagger.v3.oas.annotations.Operation(summary = "Save or Update Inquiry", parameters = {
                        @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
        })
        @PostMapping
        public ResponseEntity<ApiResponse<InquiryResponse>> saveInquiry(
                        @RequestBody InquiryRequest request) {

                InquiryResponse response = inquiryService.saveOrUpdate(request);

                return ResponseEntity.ok(
                                ApiResponse.<InquiryResponse>builder()
                                                .success(true)
                                                .message("Inquiry saved successfully")
                                                .data(response)
                                                .build());
        }

        @io.swagger.v3.oas.annotations.Operation(summary = "Get all inquiries", parameters = {
                        @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
        })
        @GetMapping
        public ResponseEntity<ApiResponse<List<InquiryResponse>>> getAllInquiries() {

                return ResponseEntity.ok(
                                ApiResponse.<List<InquiryResponse>>builder()
                                                .success(true)
                                                .message("Inquiry list fetched successfully")
                                                .data(inquiryService.getAll())
                                                .build());
        }

        @io.swagger.v3.oas.annotations.Operation(summary = "Delete inquiry", parameters = {
                        @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteInquiry(@PathVariable Long id) {

                inquiryService.delete(id);

                return ResponseEntity.ok(
                                ApiResponse.<Void>builder()
                                                .success(true)
                                                .message("Inquiry deleted successfully")
                                                .build());
        }

        // ---------------- Follow-Up Dashboard ----------------
        @io.swagger.v3.oas.annotations.Operation(summary = "Get dashboard inquiries", parameters = {
                        @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
        })
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
                                                .build());
        }

        // ---------------- Inquiry Summary ----------------
        @io.swagger.v3.oas.annotations.Operation(summary = "Get inquiry summary", parameters = {
                        @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
        })
        @GetMapping("/{inquiryId}/summary")
        public ResponseEntity<ApiResponse<InquirySummaryResponse>> getInquirySummary(@PathVariable Long inquiryId) {
                InquirySummaryResponse summary = inquiryService.getInquirySummary(inquiryId);

                return ResponseEntity.ok(
                                ApiResponse.<InquirySummaryResponse>builder()
                                                .success(true)
                                                .message("Inquiry summary fetched")
                                                .data(summary)
                                                .build());
        }

        // ---------------- Follow-Up History ----------------
        @io.swagger.v3.oas.annotations.Operation(summary = "Get follow-up history", parameters = {
                        @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
        })
        @GetMapping("/{inquiryId}/follow-ups")
        public ResponseEntity<ApiResponse<List<FollowUpResponse>>> getFollowUps(@PathVariable Long inquiryId) {
                List<FollowUpResponse> list = followUpService.getFollowUps(inquiryId);

                return ResponseEntity.ok(
                                ApiResponse.<List<FollowUpResponse>>builder()
                                                .success(true)
                                                .message("Follow-up history fetched")
                                                .data(list)
                                                .build());
        }

        // ---------------- Add Follow-Up ----------------
        @io.swagger.v3.oas.annotations.Operation(summary = "Add follow-up", parameters = {
                        @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
        })
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
                                                .build());
        }

        // ---------------- Proceed to Admission ----------------
        @io.swagger.v3.oas.annotations.Operation(summary = "Proceed to admission", parameters = {
                        @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
        })
        @PostMapping("/{inquiryId}/proceed-admission")
        public ResponseEntity<ApiResponse<Void>> proceedToAdmission(@PathVariable Long inquiryId) {
                inquiryService.proceedToAdmission(inquiryId);

                return ResponseEntity.ok(
                                ApiResponse.<Void>builder()
                                                .success(true)
                                                .message("Inquiry marked ready for admission")
                                                .build());
        }

        // ---------------- Mark Lost ----------------
        @io.swagger.v3.oas.annotations.Operation(summary = "Mark inquiry as lost", parameters = {
                        @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
        })
        @PostMapping("/{inquiryId}/mark-lost")
        public ResponseEntity<ApiResponse<Void>> markLost(@PathVariable Long inquiryId) {
                inquiryService.markLost(inquiryId);

                return ResponseEntity.ok(
                                ApiResponse.<Void>builder()
                                                .success(true)
                                                .message("Inquiry marked as lost")
                                                .build());
        }

}
