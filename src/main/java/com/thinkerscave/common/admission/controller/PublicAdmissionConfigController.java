package com.thinkerscave.common.admission.controller;

import com.thinkerscave.common.admission.dto.AdmissionFormConfigResponse;
import com.thinkerscave.common.admission.service.AdmissionFormConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/public/admissions/form-config")
@RequiredArgsConstructor
@Tag(name = "Public Admission Config", description = "Public configuration resources for rendering the admission form")
public class PublicAdmissionConfigController {

    private final AdmissionFormConfigService configService;

    @GetMapping
    @Operation(summary = "Get Admission Form Configuration")
    public AdmissionFormConfigResponse getFormConfig() {
        return configService.getFormConfigForCurrentTenant();
    }
}
