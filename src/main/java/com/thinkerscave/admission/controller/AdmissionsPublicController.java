package com.thinkerscave.admission.controller;

import com.thinkerscave.academics.dto.response.LookupDTO;
import com.thinkerscave.academics.service.AcademicsLookupService;
import com.thinkerscave.admission.dto.request.InquiryRequest;
import com.thinkerscave.admission.dto.request.PublicAdmissionInquiryRequest;
import com.thinkerscave.admission.dto.response.InquiryResponse;
import com.thinkerscave.admission.dto.response.PublicAdmissionsFormConfig;
import com.thinkerscave.admission.enums.LeadSource;
import com.thinkerscave.admission.service.InquiryService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.exceptions.BadRequestException;
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

    @PostMapping("/inquiries")
    @Operation(summary = "Submit an admission enquiry from the public website")
    public ResponseEntity<ApiResponse<InquiryResponse>> createInquiry(@Valid @RequestBody PublicAdmissionInquiryRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Enquiry submitted successfully",
                inquiryService.create(toInquiryRequest(request))));
    }

    private InquiryRequest toInquiryRequest(PublicAdmissionInquiryRequest request) {
        InquiryRequest mapped = new InquiryRequest();
        mapped.setName(StringUtils.hasText(request.getStudentName()) ? request.getStudentName().trim() : "");
        mapped.setParentContactName(StringUtils.hasText(request.getStudentName()) ? request.getStudentName().trim() : "Website Enquiry");
        mapped.setMobileNumber(request.getMobileNumber().trim());

        String className = null;
        Long academicYearId = null;
        for (LookupDTO year : academicsLookupService.getActiveAcademicYears()) {
            for (LookupDTO cls : academicsLookupService.getClassesByYear(year.getId())) {
                if (cls.getId().equals(request.getClassId())) {
                    className = cls.getName();
                    academicYearId = year.getId();
                    break;
                }
            }
            if (className != null) {
                break;
            }
        }
        if (!StringUtils.hasText(className)) {
            throw new BadRequestException("Please select a valid class");
        }
        mapped.setAcademicYearId(academicYearId);
        mapped.setClassId(request.getClassId());
        mapped.setClassInterestedIn(className);
        mapped.setInquirySource(LeadSource.WEBSITE);
        return mapped;
    }
}