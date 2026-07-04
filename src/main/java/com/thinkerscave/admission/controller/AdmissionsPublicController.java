package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.InquiryRequest;
import com.thinkerscave.admission.dto.request.PublicInquiryRequest;
import com.thinkerscave.admission.dto.response.InquiryResponse;
import com.thinkerscave.admission.service.InquiryService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/admissions")
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Public")
public class AdmissionsPublicController {

    private final InquiryService inquiryService;

    @PostMapping("/inquiry")
    @Operation(summary = "Create inquiry from public website form")
    public ResponseEntity<ApiResponse<InquiryResponse>> createPublicInquiry(@Valid @RequestBody PublicInquiryRequest request) {
        InquiryRequest mapped = new InquiryRequest();
        mapped.setName(request.getName());
        mapped.setMobileNumber(request.getMobileNumber());
        mapped.setEmail(request.getEmail());
        mapped.setClassInterestedIn(request.getClassInterestedIn());
        mapped.setAddress(request.getAddress());
        mapped.setInquirySource(request.getInquirySource());
        mapped.setComments(request.getComments());
        return ResponseEntity.ok(ApiResponse.created("Inquiry submitted", inquiryService.create(mapped)));
    }
}