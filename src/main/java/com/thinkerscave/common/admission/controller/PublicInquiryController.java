package com.thinkerscave.common.admission.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thinkerscave.common.admission.dto.PublicInquiryRequestDTO;
import com.thinkerscave.common.admission.service.InquiryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public/inquiries")
@Tag(name = "Public Inquiry", description = "Endpoints for public inquiries")
@RequiredArgsConstructor
public class PublicInquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "Submit an Inquiry", description = "Public endpoint for users to submit an inquiry.")
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitInquiry(
            @Valid @RequestBody PublicInquiryRequestDTO request) {

        String message = inquiryService.createPublicInquiry(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);

        return ResponseEntity.ok(response);
    }
}
