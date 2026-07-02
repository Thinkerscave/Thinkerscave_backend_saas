package com.thinkerscave.student.controller;

import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.dto.TransferStatusUpdateRequest;
import com.thinkerscave.student.enums.TransferStatus;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.TransferRequestRepository;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/v1/student-movements", "/api/v1/students/transfers"})
@Tag(name = "Student Transfer Management")
@RequiredArgsConstructor
@Slf4j
public class StudentMovementController {

    private final TransferRequestRepository transferRequestRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
    public ResponseEntity<ApiResponse<java.util.List<com.thinkerscave.student.dto.TransferRequestDTO>>> listTransfers() {
        Long orgId = OrganizationContext.getOrganizationId();
        java.util.List<com.thinkerscave.student.dto.TransferRequestDTO> dtoList = transferRequestRepository
            .findAllWithEnrollmentByOrganizationId(orgId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Transfer list loaded", dtoList));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
    public ResponseEntity<ApiResponse<com.thinkerscave.student.dto.TransferRequestDTO>> createTransfer(
            @RequestBody com.thinkerscave.student.dto.TransferRequestDTO dto) {
        Long orgId = OrganizationContext.getOrganizationId();
        com.thinkerscave.student.entity.TransferRequest req = new com.thinkerscave.student.entity.TransferRequest();
        req.setRequestNumber("TRF-" + System.currentTimeMillis());
        req.setStudentId(dto.getStudentId());
        req.setReason(dto.getReason());
        req.setDestinationSchool(dto.getDestinationSchool());
        req.setRequestedOn(java.time.LocalDate.now());
        req.setStatus(TransferStatus.REQUESTED);
        req.setOrganizationId(orgId);

        req.setEnrollment(studentEnrollmentRepository.findById(dto.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + dto.getEnrollmentId())));
        
        com.thinkerscave.student.entity.TransferRequest saved = transferRequestRepository.save(req);
        return ResponseEntity.status(201).body(ApiResponse.success("Transfer request created", mapToDTO(saved)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF')")
    public ResponseEntity<ApiResponse<com.thinkerscave.student.dto.TransferRequestDTO>> transitionTransferStatus(
            @PathVariable Long id,
            @Valid @RequestBody TransferStatusUpdateRequest request) {
        com.thinkerscave.student.entity.TransferRequest req = transferRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer request not found: " + id));
        req.setStatus(request.getStatus());
        req.setRemarks(request.getRemarks());

        if (TransferStatus.APPROVED == request.getStatus()) {
            req.setApprovedOn(java.time.LocalDate.now());
            req.setCertificateNumber("CERT-" + System.currentTimeMillis());
            req.setCertificateIssuedOn(java.time.LocalDate.now());
        }

        if (TransferStatus.CERTIFICATE_ISSUED == request.getStatus() && req.getCertificateNumber() == null) {
            req.setCertificateNumber("CERT-" + System.currentTimeMillis());
            req.setCertificateIssuedOn(java.time.LocalDate.now());
        }

        com.thinkerscave.student.entity.TransferRequest saved = transferRequestRepository.save(req);
        return ResponseEntity.ok(ApiResponse.success("Transfer request updated", mapToDTO(saved)));
    }

    @GetMapping("/{id}/certificate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','HR_MANAGER','PRINCIPAL','STAFF','TEACHER')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getTransferCertificate(@PathVariable Long id) {
        com.thinkerscave.student.entity.TransferRequest req = transferRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer request not found: " + id));

        if (req.getCertificateNumber() == null) {
            throw new IllegalArgumentException("Certificate is not generated yet for this request");
        }

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("requestId", req.getId());
        payload.put("requestNumber", req.getRequestNumber());
        payload.put("certificateNumber", req.getCertificateNumber());
        payload.put("certificateIssuedOn", req.getCertificateIssuedOn());
        payload.put("studentId", req.getStudentId());
        payload.put("status", req.getStatus().name());
        payload.put("destinationSchool", req.getDestinationSchool());
        return ResponseEntity.ok(ApiResponse.success("Transfer certificate generated", payload));
    }

    private com.thinkerscave.student.dto.TransferRequestDTO mapToDTO(com.thinkerscave.student.entity.TransferRequest t) {
        com.thinkerscave.student.dto.TransferRequestDTO dto = new com.thinkerscave.student.dto.TransferRequestDTO();
        dto.setId(t.getId());
        dto.setRequestNumber(t.getRequestNumber());
        dto.setStudentId(t.getStudentId());
        dto.setEnrollmentId(t.getEnrollment() != null ? t.getEnrollment().getEnrollmentId() : null);
        dto.setReason(t.getReason());
        dto.setDestinationSchool(t.getDestinationSchool());
        dto.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        dto.setRequestedOn(t.getRequestedOn());
        dto.setCertificateNumber(t.getCertificateNumber());
        return dto;
    }
}
