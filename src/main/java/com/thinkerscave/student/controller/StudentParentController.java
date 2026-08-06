package com.thinkerscave.student.controller;

import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.dto.ParentDTO;
import com.thinkerscave.student.entity.Parent;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.enums.ParentRelationship;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;

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

        var student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        Parent parent = new Parent();
        parent.setParentCode("PAR-" + System.currentTimeMillis());
        parent.setFirstName(request.getFirstName());
        parent.setMiddleName(request.getMiddleName());
        parent.setLastName(request.getLastName());
        parent.setGender(request.getGender());
        parent.setMobileNumber(request.getMobileNumber());
        parent.setEmail(request.getEmail());
        parent.setOccupation(request.getOccupation());
        parent.setQualification(request.getQualification());
        parent = parentRepository.save(parent);

        StudentParent sp = new StudentParent();
        sp.setStudent(student);
        sp.setParent(parent);
        sp.setRelationship(request.getRelationship() != null
                ? ParentRelationship.valueOf(request.getRelationship()) : ParentRelationship.GUARDIAN);
        sp.setPrimaryContact(Boolean.TRUE.equals(request.getPrimaryContact()));
        sp.setReceiveSms(Boolean.TRUE.equals(request.getReceiveSms()));
        sp.setReceiveEmail(Boolean.TRUE.equals(request.getReceiveEmail()));
        sp.setPickupAuthorized(Boolean.TRUE.equals(request.getPickupAuthorized()));
        sp.setActive(true);
        studentParentRepository.save(sp);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Parent added successfully", toDTO(parent, sp)));
    }

    @PutMapping("/{parentId}")
    @Operation(summary = "Update parent information")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Transactional
    public ResponseEntity<ApiResponse<ParentDTO>> updateParent(
            @PathVariable Long studentId,
            @PathVariable Long parentId,
            @Valid @RequestBody ParentRequest request) {

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found: " + parentId));
        parent.setFirstName(request.getFirstName());
        parent.setMiddleName(request.getMiddleName());
        parent.setLastName(request.getLastName());
        parent.setGender(request.getGender());
        parent.setMobileNumber(request.getMobileNumber());
        parent.setEmail(request.getEmail());
        parent.setOccupation(request.getOccupation());
        parent.setQualification(request.getQualification());
        parent = parentRepository.save(parent);

        StudentParent sp = studentParentRepository.findByStudentStudentId(studentId)
                .stream()
                .filter(r -> r.getParent().getParentId().equals(parentId))
                .findFirst()
                .orElse(null);

        return ResponseEntity.ok(ApiResponse.success("Parent updated", toDTO(parent, sp)));
    }

    private ParentDTO toDTO(Parent p, StudentParent sp) {
        ParentDTO dto = new ParentDTO();
        dto.setParentId(p.getParentId());
        dto.setParentCode(p.getParentCode());
        dto.setFullName(p.getFirstName() + " " + p.getLastName());
        dto.setMobileNumber(p.getMobileNumber());
        dto.setEmail(p.getEmail());
        dto.setOccupation(p.getOccupation());
        return dto;
    }

    @Data
    public static class ParentRequest {
        @NotBlank
        private String firstName;
        private String middleName;
        @NotBlank
        private String lastName;
        private String gender;
        @NotBlank
        private String mobileNumber;
        private String email;
        private String occupation;
        private String qualification;
        private String relationship;
        private Boolean primaryContact;
        private Boolean receiveSms;
        private Boolean receiveEmail;
        private Boolean pickupAuthorized;
    }
}
