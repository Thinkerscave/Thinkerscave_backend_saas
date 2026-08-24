package com.thinkerscave.admission.controller;

import com.thinkerscave.academics.dto.response.LookupDTO;
import com.thinkerscave.academics.service.AcademicsLookupService;
import com.thinkerscave.admission.dto.request.InquiryRequest;
import com.thinkerscave.admission.dto.request.PublicInquiryRequest;
import com.thinkerscave.admission.dto.response.InquiryResponse;
import com.thinkerscave.admission.dto.response.PublicAdmissionsFormConfig;
import com.thinkerscave.admission.service.InquiryService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/admissions")
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Public")
public class AdmissionsPublicController {

    private final InquiryService inquiryService;
    private final AcademicsLookupService academicsLookupService;

    @GetMapping("/form-config")
    @Operation(summary = "Public inquiry form configuration for the current tenant")
    public ResponseEntity<ApiResponse<PublicAdmissionsFormConfig>> formConfig() {
        List<LookupDTO> years = academicsLookupService.getActiveAcademicYears();
        LookupDTO defaultYear = years.isEmpty() ? null : years.get(0);
        List<LookupDTO> classes = defaultYear == null
                ? List.of()
                : academicsLookupService.getClassesByYear(defaultYear.getId());
        return ResponseEntity.ok(ApiResponse.success("Form configuration loaded",
                PublicAdmissionsFormConfig.builder()
                        .defaultAcademicYearId(defaultYear != null ? defaultYear.getId() : null)
                        .academicYears(years)
                        .classes(classes)
                        .build()));
    }

    @GetMapping("/classes")
    @Operation(summary = "Public class lookup for the current tenant")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> classes(@RequestParam(required = false) Long academicYearId) {
        List<LookupDTO> years = academicsLookupService.getActiveAcademicYears();
        Long yearId = academicYearId;
        if (yearId == null && !years.isEmpty()) {
            yearId = years.get(0).getId();
        }
        List<LookupDTO> classes = yearId == null ? List.of() : academicsLookupService.getClassesByYear(yearId);
        return ResponseEntity.ok(ApiResponse.success("Classes loaded", classes));
    }

    @PostMapping("/inquiry")
    @Operation(summary = "Create inquiry from public website form")
    public ResponseEntity<ApiResponse<InquiryResponse>> createPublicInquiry(@Valid @RequestBody PublicInquiryRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Inquiry submitted", inquiryService.create(toInquiryRequest(request))));
    }

    @PostMapping("/inquiries")
    @Operation(summary = "Alias for public inquiry submission")
    public ResponseEntity<ApiResponse<InquiryResponse>> createPublicInquiryAlias(@Valid @RequestBody PublicInquiryRequest request) {
        return createPublicInquiry(request);
    }

    private InquiryRequest toInquiryRequest(PublicInquiryRequest request) {
        InquiryRequest mapped = new InquiryRequest();
        mapped.setName(request.getName());
        mapped.setMobileNumber(request.getMobileNumber());
        mapped.setEmail(request.getEmail());
        mapped.setAcademicYearId(request.getAcademicYearId());
        mapped.setClassId(request.getClassId());
        mapped.setAddress(request.getAddress());
        mapped.setInquirySource(StringUtils.hasText(request.getInquirySource()) ? request.getInquirySource() : "Website");
        mapped.setComments(request.getComments());

        String className = request.getClassInterestedIn();
        if (!StringUtils.hasText(className) && request.getClassId() != null && request.getAcademicYearId() != null) {
            className = academicsLookupService.getClassesByYear(request.getAcademicYearId()).stream()
                    .filter(item -> request.getClassId().equals(item.getId()))
                    .map(LookupDTO::getName)
                    .findFirst()
                    .orElse(null);
        }
        if (!StringUtils.hasText(className)) {
            throw new com.thinkerscave.shared.exceptions.BadRequestException("Please select a class");
        }
        mapped.setClassInterestedIn(className);
        return mapped;
    }
}
