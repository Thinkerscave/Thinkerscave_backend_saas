package com.thinkerscave.student.controller;

import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.student.enums.TransferStatus;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.TransferRequestRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/students/transfers")
@Tag(name = "Student Transfer Management")
@RequiredArgsConstructor
@Slf4j
public class StudentMovementController {

    private final TransferRequestRepository transferRequestRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<com.thinkerscave.student.dto.TransferRequestDTO>>> listTransfers() {
        Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
        java.util.List<com.thinkerscave.student.dto.TransferRequestDTO> dtoList = transferRequestRepository.findAll().stream()
            .filter(t -> t.getOrganizationId() != null && t.getOrganizationId().equals(orgId))
            .map(this::mapToDTO)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Transfer list loaded", dtoList));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<com.thinkerscave.student.dto.TransferRequestDTO>> createTransfer(
            @RequestBody com.thinkerscave.student.dto.TransferRequestDTO dto) {
        Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
        com.thinkerscave.student.entity.TransferRequest req = new com.thinkerscave.student.entity.TransferRequest();
        req.setRequestNumber("TRF-" + System.currentTimeMillis());
        req.setStudentId(dto.getStudentId());
        req.setReason(dto.getReason());
        req.setDestinationSchool(dto.getDestinationSchool());
        req.setRequestedOn(java.time.LocalDate.now());
        req.setStatus(TransferStatus.UNDER_REVIEW);
        req.setOrganizationId(orgId);

        studentEnrollmentRepository.findById(dto.getEnrollmentId()).ifPresent(req::setEnrollment);
        
        com.thinkerscave.student.entity.TransferRequest saved = transferRequestRepository.save(req);
        return ResponseEntity.ok(ApiResponse.success("Transfer request created", mapToDTO(saved)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<com.thinkerscave.student.dto.TransferRequestDTO>> transitionTransferStatus(
            @PathVariable Long id, @RequestBody com.thinkerscave.student.dto.TransferRequestDTO dto) {
        com.thinkerscave.student.entity.TransferRequest req = transferRequestRepository.findById(id).orElseThrow();
        req.setStatus(com.thinkerscave.student.enums.TransferStatus.valueOf(dto.getStatus()));
        if (com.thinkerscave.student.enums.TransferStatus.APPROVED.name().equals(dto.getStatus())) {
            req.setApprovedOn(java.time.LocalDate.now());
            req.setCertificateNumber("CERT-" + System.currentTimeMillis());
            req.setCertificateIssuedOn(java.time.LocalDate.now());
        }
        com.thinkerscave.student.entity.TransferRequest saved = transferRequestRepository.save(req);
        return ResponseEntity.ok(ApiResponse.success("Transfer request updated", mapToDTO(saved)));
    }

    private com.thinkerscave.student.dto.TransferRequestDTO mapToDTO(com.thinkerscave.student.entity.TransferRequest t) {
        com.thinkerscave.student.dto.TransferRequestDTO dto = new com.thinkerscave.student.dto.TransferRequestDTO();
        dto.setId(t.getId());
        dto.setRequestNumber(t.getRequestNumber());
        dto.setStudentId(t.getStudentId());
        dto.setEnrollmentId(t.getEnrollment() != null ? t.getEnrollment().getEnrollmentId() : null);
        dto.setReason(t.getReason());
        dto.setDestinationSchool(t.getDestinationSchool());
        dto.setStatus(t.getStatus().name());
        dto.setRequestedOn(t.getRequestedOn());
        dto.setCertificateNumber(t.getCertificateNumber());
        return dto;
    }
}
