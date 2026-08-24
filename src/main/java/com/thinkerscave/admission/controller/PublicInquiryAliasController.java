package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.PublicInquiryRequest;
import com.thinkerscave.admission.dto.response.InquiryResponse;
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
@RequestMapping("/api/v1/public/inquiries")
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Public Inquiry Alias")
public class PublicInquiryAliasController {

    private final AdmissionsPublicController publicController;

    @PostMapping
    @Operation(summary = "Create public inquiry (compatibility path)")
    public ResponseEntity<ApiResponse<InquiryResponse>> create(@Valid @RequestBody PublicInquiryRequest request) {
        return publicController.createPublicInquiry(request);
    }
}
