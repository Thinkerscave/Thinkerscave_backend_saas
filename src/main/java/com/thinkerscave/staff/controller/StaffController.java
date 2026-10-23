package com.thinkerscave.staff.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.staff.dto.request.StaffBankDetailsRequest;
import com.thinkerscave.staff.dto.request.StaffCreateRequest;
import com.thinkerscave.staff.dto.request.StaffUpdateRequest;
import com.thinkerscave.staff.dto.response.DocumentResponse;
import com.thinkerscave.staff.dto.response.StaffActivityResponse;
import com.thinkerscave.staff.dto.response.StaffBankDetailsResponse;
import com.thinkerscave.staff.dto.response.StaffCreateResponse;
import com.thinkerscave.staff.dto.response.StaffDashboardResponse;
import com.thinkerscave.staff.dto.response.StaffDetailResponse;
import com.thinkerscave.staff.dto.response.StaffSummaryResponse;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import com.thinkerscave.staff.security.StaffAccessGuard;
import com.thinkerscave.staff.service.StaffBankDetailsService;
import com.thinkerscave.staff.service.StaffActivityService;
import com.thinkerscave.staff.service.StaffDocumentService;
import com.thinkerscave.staff.service.StaffExportService;
import com.thinkerscave.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Management", description = "APIs for managing staff members")
public class StaffController {

    private final StaffService staffService;
    private final StaffBankDetailsService bankDetailsService;
    private final StaffDocumentService documentService;
    private final StaffExportService exportService;
    private final StaffActivityService activityService;
    private final StaffAccessGuard accessGuard;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new staff member")
    public ResponseEntity<ApiResponse<StaffCreateResponse>> createStaff(
            @Valid @RequestBody StaffCreateRequest request) {
        accessGuard.requireWrite();
        StaffCreateResponse response = staffService.createStaff(request);
        return ResponseEntity.status(201).body(ApiResponse.created("Staff created successfully", response));
    }

    @PutMapping("/{staffId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update staff details")
    public ResponseEntity<ApiResponse<Void>> updateStaff(
            @PathVariable Long staffId,
            @Valid @RequestBody StaffUpdateRequest request) {
        accessGuard.requireWrite();
        staffService.updateStaff(staffId, request);
        return ResponseEntity.ok(ApiResponse.noContent("Staff updated successfully"));
    }

    @GetMapping("/{staffId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get staff 360 profile details")
    public ResponseEntity<ApiResponse<StaffDetailResponse>> getStaffDetail(@PathVariable Long staffId) {
        accessGuard.requireView();
        return ResponseEntity.ok(ApiResponse.success("Staff details retrieved", staffService.getStaffDetail(staffId)));
    }

    @GetMapping("/{staffId}/activity")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get staff activity log (latest first)")
    public ResponseEntity<ApiResponse<List<StaffActivityResponse>>> getStaffActivity(@PathVariable Long staffId) {
        accessGuard.requireView();
        return ResponseEntity.ok(ApiResponse.success("Staff activity retrieved", activityService.list(staffId)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get paginated staff list with filters")
    public ResponseEntity<ApiResponse<Page<StaffSummaryResponse>>> getStaffList(
            @RequestParam(required = false) StaffType staffType,
            @RequestParam(required = false) EmploymentCategory employmentCategory,
            @RequestParam(required = false) EmploymentStatus employmentStatus,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdOn") Pageable pageable) {
        accessGuard.requireView();
        Page<StaffSummaryResponse> page = staffService.getStaffList(
                staffType, employmentCategory, employmentStatus, designation, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Staff list retrieved", page));
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Export the filtered staff list as an Excel workbook")
    public ResponseEntity<byte[]> exportStaff(
            @RequestParam(required = false) StaffType staffType,
            @RequestParam(required = false) EmploymentCategory employmentCategory,
            @RequestParam(required = false) EmploymentStatus employmentStatus,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String keyword) {
        accessGuard.requireView();
        byte[] workbook = exportService.exportExcel(
                staffType, employmentCategory, employmentStatus, designation, keyword);
        String fileName = "staff-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(workbook);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get staff dashboard KPI metrics")
    public ResponseEntity<ApiResponse<StaffDashboardResponse>> getDashboard() {
        accessGuard.requireView();
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved", staffService.getDashboard()));
    }

    @PatchMapping("/{staffId}/activate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Activate a staff member")
    public ResponseEntity<ApiResponse<Void>> activateStaff(@PathVariable Long staffId) {
        accessGuard.requireWrite();
        staffService.activateStaff(staffId);
        return ResponseEntity.ok(ApiResponse.noContent("Staff activated successfully"));
    }

    @PatchMapping("/{staffId}/deactivate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Deactivate a staff member")
    public ResponseEntity<ApiResponse<Void>> deactivateStaff(@PathVariable Long staffId) {
        accessGuard.requireWrite();
        staffService.deactivateStaff(staffId);
        return ResponseEntity.ok(ApiResponse.noContent("Staff deactivated successfully"));
    }

    @DeleteMapping("/{staffId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Soft delete a staff member")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable Long staffId) {
        accessGuard.requireWrite();
        staffService.deleteStaff(staffId);
        return ResponseEntity.ok(ApiResponse.noContent("Staff deleted successfully"));
    }

    @GetMapping("/{staffId}/bank-details")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get masked bank details for a staff member")
    public ResponseEntity<ApiResponse<StaffBankDetailsResponse>> getBankDetails(@PathVariable Long staffId) {
        accessGuard.requireView();
        StaffBankDetailsResponse response = bankDetailsService.findMasked(staffId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Bank details retrieved", response));
    }

    @PutMapping("/{staffId}/bank-details")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create or replace bank details (account number stored encrypted)")
    public ResponseEntity<ApiResponse<StaffBankDetailsResponse>> upsertBankDetails(
            @PathVariable Long staffId,
            @Valid @RequestBody StaffBankDetailsRequest request) {
        accessGuard.requireWrite();
        StaffBankDetailsResponse response = bankDetailsService.upsert(staffId, request);
        return ResponseEntity.ok(ApiResponse.success("Bank details saved", response));
    }

    @GetMapping("/{staffId}/documents")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List active documents for a staff member")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listDocuments(@PathVariable Long staffId) {
        accessGuard.requireView();
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved", documentService.list(staffId)));
    }

    @PostMapping(value = "/{staffId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload a document for a staff member")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long staffId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "documentName", required = false) String documentName) {
        accessGuard.requireWrite();
        DocumentResponse response = documentService.upload(staffId, file, documentType, documentName);
        return ResponseEntity.status(201).body(ApiResponse.created("Document uploaded", response));
    }

    @PutMapping(value = "/{staffId}/documents/{documentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Replace a staff document (previous version is kept inactive)")
    public ResponseEntity<ApiResponse<DocumentResponse>> replaceDocument(
            @PathVariable Long staffId,
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName) {
        accessGuard.requireWrite();
        DocumentResponse response = documentService.replace(staffId, documentId, file, documentName);
        return ResponseEntity.ok(ApiResponse.success("Document replaced", response));
    }

    @GetMapping("/{staffId}/documents/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download a staff document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long staffId, @PathVariable Long documentId) {
        accessGuard.requireView();
        StaffDocumentService.DownloadedDocument doc = documentService.download(staffId, documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(doc.fileName()).build().toString())
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .body(doc.resource());
    }

    @DeleteMapping("/{staffId}/documents/{documentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Deactivate a staff document")
    public ResponseEntity<ApiResponse<Void>> deactivateDocument(@PathVariable Long staffId, @PathVariable Long documentId) {
        accessGuard.requireWrite();
        documentService.deactivate(staffId, documentId);
        return ResponseEntity.ok(ApiResponse.noContent("Document removed"));
    }

    @PostMapping(value = "/{staffId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload or replace a staff profile photo (JPG, PNG or WEBP, max 2 MB)")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadPhoto(
            @PathVariable Long staffId,
            @RequestParam("file") MultipartFile file) {
        accessGuard.requireWrite();
        return ResponseEntity.ok(ApiResponse.success("Photo saved", documentService.uploadPhoto(staffId, file)));
    }

    @GetMapping("/{staffId}/photo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a staff profile photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable Long staffId) {
        accessGuard.requireView();
        StaffDocumentService.DownloadedDocument photo = documentService.loadPhoto(staffId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .body(photo.resource());
    }

    @DeleteMapping("/{staffId}/photo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove a staff profile photo")
    public ResponseEntity<ApiResponse<Void>> removePhoto(@PathVariable Long staffId) {
        accessGuard.requireWrite();
        documentService.removePhoto(staffId);
        return ResponseEntity.ok(ApiResponse.noContent("Photo removed"));
    }
}
