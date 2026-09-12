package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.ApplicationAdmissionRequest;
import com.thinkerscave.admission.dto.request.ApplicationSearchRequest;
import com.thinkerscave.admission.dto.request.EnrollApplicationRequest;
import com.thinkerscave.admission.dto.request.RecordFeeRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.dto.response.ApplicationDocumentFile;
import com.thinkerscave.admission.dto.response.ApplicationDocumentResponse;
import com.thinkerscave.admission.dto.response.ApplicationProgressResponse;
import com.thinkerscave.admission.dto.response.EnrollmentResultResponse;
import com.thinkerscave.admission.dto.response.FamilyMatchResponse;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.DocumentCheckStatus;
import com.thinkerscave.admission.service.ApplicationAdmissionService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admissions/applications")
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Applications")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
public class AdmissionsApplicationController {

    private final ApplicationAdmissionService applicationService;

    @PostMapping("/draft")
    @Operation(summary = "Save application draft")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> saveDraft(@Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Draft saved", applicationService.saveDraft(request)));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit application")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> submit(@Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Application submitted", applicationService.submit(request)));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit an existing draft application")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> submitExisting(
            @PathVariable Long id,
            @RequestBody(required = false) ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application submitted",
                applicationService.submitExisting(id, request != null ? request : new ApplicationAdmissionRequest())));
    }

    @GetMapping
    @Operation(summary = "List applications")
    public ResponseEntity<ApiResponse<Page<ApplicationAdmissionResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Applications loaded", applicationService.getAll(pageable)));
    }

    @PostMapping("/search")
    @Operation(summary = "Search applications")
    public ResponseEntity<ApiResponse<Page<ApplicationAdmissionResponse>>> search(
            @RequestBody(required = false) ApplicationSearchRequest request,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Application search completed", applicationService.search(request, pageable)));
    }

    @GetMapping("/family-match")
    @Operation(summary = "Find existing family/parent by mobile or email for sibling linking")
    public ResponseEntity<ApiResponse<FamilyMatchResponse>> familyMatch(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(ApiResponse.success("Family match result",
                applicationService.findFamilyMatch(mobile, email)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application detail")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application loaded", applicationService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update application")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application updated", applicationService.update(id, request)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve application")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success("Application approved", applicationService.approve(id, remarks)));
    }

    @PostMapping("/{id}/request-correction")
    @Operation(summary = "Send application back for correction (ACTION_REQUIRED)")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> requestCorrection(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success("Correction requested",
                applicationService.requestCorrection(id, reason)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject application")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success("Application rejected", applicationService.reject(id, remarks)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update application status")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success("Application status updated", applicationService.updateStatus(id, status, remarks)));
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Get wizard progress")
    public ResponseEntity<ApiResponse<ApplicationProgressResponse>> progress(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application progress loaded", applicationService.getProgress(id)));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive application (DRAFT/SUBMITTED only)")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> archive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application archived", applicationService.archive(id)));
    }

    @PostMapping("/{id}/unarchive")
    @Operation(summary = "Unarchive application")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> unarchive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application unarchived", applicationService.unarchive(id)));
    }

    @PostMapping("/{id}/fee")
    @Operation(summary = "Record admission fee payment")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> recordFee(
            @PathVariable Long id,
            @Valid @RequestBody RecordFeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fee recorded", applicationService.recordFee(id, request)));
    }

    @PostMapping("/{id}/enroll")
    @Operation(summary = "Enroll approved application and create student")
    public ResponseEntity<ApiResponse<EnrollmentResultResponse>> enroll(
            @PathVariable Long id,
            @Valid @RequestBody EnrollApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment completed successfully", applicationService.enroll(id, request)));
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "List application documents")
    public ResponseEntity<ApiResponse<List<ApplicationDocumentResponse>>> listDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Documents loaded", applicationService.listDocuments(id)));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload application document")
    public ResponseEntity<ApiResponse<ApplicationDocumentResponse>> uploadDocument(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.created("Document uploaded",
                applicationService.uploadDocument(id, file, documentType, remarks)));
    }

    @PostMapping("/documents/{documentId}/verify")
    @Operation(summary = "Verify or reject an application document")
    public ResponseEntity<ApiResponse<ApplicationDocumentResponse>> updateDocumentStatus(
            @PathVariable Long documentId,
            @RequestParam DocumentCheckStatus status,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success("Document status updated",
                applicationService.updateDocumentStatus(documentId, status, remarks)));
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Delete an application document")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long documentId) {
        applicationService.deleteDocument(documentId);
        return ResponseEntity.ok(ApiResponse.noContent("Document deleted"));
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "Download / preview an application document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        ApplicationDocumentFile file = applicationService.downloadDocument(documentId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.originalName(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .body(file.resource());
    }
}
