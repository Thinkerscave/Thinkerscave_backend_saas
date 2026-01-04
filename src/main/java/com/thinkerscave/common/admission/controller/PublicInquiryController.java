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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/inquiries")
@RequiredArgsConstructor
public class PublicInquiryController {

    private final InquiryService inquiryService;

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
