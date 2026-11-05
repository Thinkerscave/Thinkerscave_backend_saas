package com.thinkerscave.student.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.student.dto.ParentDTO;
import com.thinkerscave.student.dto.StudentParentRequest;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/students/{studentId}/parents")
@Tag(name = "Student Parent Management", description = "APIs for managing student parent/guardian relationships")
@RequiredArgsConstructor
@Slf4j
public class StudentParentController {

    private final StudentParentRepository studentParentRepository;
        private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Get all parents/guardians for a student")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<List<ParentDTO>>> getParents(@PathVariable Long studentId) {
        List<ParentDTO> parents = studentParentRepository.findByStudentStudentId(studentId)
                .stream()
                .map(sp -> toDTO(sp.getParent(), sp))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Parents retrieved", parents));
    }

    @PostMapping
    @Operation(summary = "Add a parent/guardian to a student")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Transactional
    public ResponseEntity<ApiResponse<ParentDTO>> addParent(
            @PathVariable Long studentId,
            @Valid @RequestBody ParentRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created("Parent added successfully",
                studentService.addParent(studentId, toRequest(request))));
    }

    @PutMapping("/{parentId}")
    @Operation(summary = "Update parent information")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Transactional
    public ResponseEntity<ApiResponse<ParentDTO>> updateParent(
            @PathVariable Long studentId,
            @PathVariable Long parentId,
            @Valid @RequestBody ParentRequest request) {

                return ResponseEntity.ok(ApiResponse.success("Parent updated",
                                studentService.updateParent(studentId, parentId, toRequest(request))));
    }

        @DeleteMapping("/{parentId}")
        @Operation(summary = "Remove a parent/guardian relationship without deleting the parent record")
        @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
        public ResponseEntity<ApiResponse<Void>> removeParent(@PathVariable Long studentId, @PathVariable Long parentId) {
                studentService.removeParent(studentId, parentId);
                return ResponseEntity.ok(ApiResponse.noContent("Parent relationship removed"));
        }

        private StudentParentRequest toRequest(ParentRequest request) {
                StudentParentRequest dto = new StudentParentRequest();
                dto.setExistingParentId(request.getExistingParentId());
                dto.setRelationship(request.getRelationship());
                dto.setFirstName(request.getFirstName());
                dto.setMiddleName(request.getMiddleName());
                dto.setLastName(request.getLastName());
                dto.setMobileNumber(request.getMobileNumber());
                dto.setEmail(request.getEmail());
                dto.setOccupation(request.getOccupation());
                dto.setOrganizationName(request.getOrganizationName());
                dto.setQualification(request.getQualification());
                dto.setAnnualIncome(request.getAnnualIncome());
                dto.setPrimaryContact(request.getPrimaryContact());
                dto.setReceiveSms(request.getReceiveSms());
                dto.setReceiveEmail(request.getReceiveEmail());
                dto.setPickupAuthorized(request.getPickupAuthorized());
                return dto;
        }

        private ParentDTO toDTO(com.thinkerscave.student.entity.Parent p, StudentParent sp) {
        ParentDTO dto = new ParentDTO();
        dto.setParentId(p.getParentId());
        dto.setParentCode(p.getParentCode());
        dto.setFullName(p.getFirstName() + " " + p.getLastName());
        dto.setMobileNumber(p.getMobileNumber());
        dto.setEmail(p.getEmail());
        dto.setOccupation(p.getOccupation());
                dto.setOrganizationName(p.getOrganizationName());
                dto.setQualification(p.getQualification());
                if (sp != null) {
                        dto.setRelationship(sp.getRelationship() != null ? sp.getRelationship().name() : null);
                        dto.setPrimaryContact(Boolean.TRUE.equals(sp.getPrimaryContact()));
                        dto.setReceiveSms(Boolean.TRUE.equals(sp.getReceiveSms()));
                        dto.setReceiveEmail(Boolean.TRUE.equals(sp.getReceiveEmail()));
                        dto.setPickupAuthorized(Boolean.TRUE.equals(sp.getPickupAuthorized()));
                }
        return dto;
    }

    @Data
    public static class ParentRequest {
        @NotBlank
        private String firstName;
        private String middleName;
        private String lastName;
        private Long existingParentId;
        private String gender;
        @NotBlank
        private String mobileNumber;
        private String email;
        private String occupation;
        private String organizationName;
        private String qualification;
        private Double annualIncome;
        private String relationship;
        private Boolean primaryContact;
        private Boolean receiveSms;
        private Boolean receiveEmail;
        private Boolean pickupAuthorized;
    }
}
