package com.thinkerscave.common.admission.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.thinkerscave.common.admission.dto.InquiryRequest;
import com.thinkerscave.common.admission.dto.InquiryResponse;
import com.thinkerscave.common.admission.service.InquiryService;
import com.thinkerscave.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/staff/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

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

}

