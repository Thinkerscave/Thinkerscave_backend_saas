package com.thinkerscave.common.admission.service;

import com.thinkerscave.common.admission.dto.ApplicationAdmissionCreateRequest;
import com.thinkerscave.common.admission.dto.ApplicationAdmissionEditRequest;
import com.thinkerscave.common.admission.dto.ApplicationAdmissionResponse;
import com.thinkerscave.common.admission.domain.ApplicationAdmission;
import com.thinkerscave.common.admission.repository.ApplicationAdmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class providing business logic for admission application operations.
 * <p>
 * This service handles creation, update, and retrieval of ApplicationAdmission entities,
 * coordinating with the repository and mapping layers.
 *
 * @author Bibekananda Pradhan
 * @since 2025-08-05
 */
@Service
@RequiredArgsConstructor
public class ApplicationAdmissionService {

    private final ApplicationAdmissionRepository repository;

    /**
     * Creates a new ApplicationAdmission entity from the provided DTO and saves it to the database.
     *
     * @param request DTO containing application data
     * @return Response DTO with the created application's details
     *
     * @author Bibekananda Pradhan
     * @since 2025-08-05
     */
    @Transactional
    public ApplicationAdmissionResponse create(ApplicationAdmissionCreateRequest request) {
        ApplicationAdmission entity = ApplicationAdmission.builder()
                .applicationId(request.getApplicationId())
                .applicantName(request.getApplicantName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .applyingForSchoolOrCollege(request.getApplyingForSchoolOrCollege())
                .parentName(request.getParentName())
                .guardianName(request.getGuardianName())
                .contactNumber(request.getContactNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPinCode())
                .emergencyContact(request.getEmergencyContact())
                .uploadedDocuments(request.getUploadedDocuments())
                .status(request.getStatus())
                .internalComments(request.getInternalComments())
                .build();
        entity = repository.save(entity);
        return toResponse(entity);
    }

    /**
     * Updates an existing ApplicationAdmission entity with data from the provided DTO.
     *
     * @param id ApplicationAdmission id
     * @param request DTO containing fields to update
     * @return Response DTO with the updated application's details, if found
     *
     * @author Bibekananda Pradhan
     * @since 2025-08-05
     */
    @Transactional
    public Optional<ApplicationAdmissionResponse> edit(Long id, ApplicationAdmissionEditRequest request) {
        return repository.findById(id).map(entity -> {
            entity.setApplicantName(request.getApplicantName());
            entity.setDateOfBirth(request.getDateOfBirth());
            entity.setGender(request.getGender());
            entity.setApplyingForSchoolOrCollege(request.getApplyingForSchoolOrCollege());
            entity.setParentName(request.getParentName());
            entity.setGuardianName(request.getGuardianName());
            entity.setContactNumber(request.getContactNumber());
            entity.setEmail(request.getEmail());
            entity.setAddress(request.getAddress());
            entity.setCity(request.getCity());
            entity.setState(request.getState());
            entity.setPincode(request.getPinCode());
            entity.setEmergencyContact(request.getEmergencyContact());
            entity.setUploadedDocuments(request.getUploadedDocuments());
            entity.setStatus(request.getStatus());
            entity.setInternalComments(request.getInternalComments());
            ApplicationAdmission updated = repository.save(entity);
            return toResponse(updated);
        });
    }

    /**
     * Retrieves an ApplicationAdmission entity by its id and maps it to a response DTO.
     *
     * @param id ApplicationAdmission id
     * @return Response DTO with the application's details, if foun
     * @author Bibekananda Pradhan
     * @since 2025-08-05
     */
    @Transactional(readOnly = true)
    public Optional<ApplicationAdmissionResponse> getById(Long id) {
        return repository.findById(id).map(this::toResponse);
    }

    /**
     * Converts an ApplicationAdmission entity to a response DTO.
     *
     * @param entity ApplicationAdmission entity
     * @return Response DTO
     * @author Bibekananda Pradhan
     * @since 2025-08-05
     */
    private ApplicationAdmissionResponse toResponse(ApplicationAdmission entity) {
        return ApplicationAdmissionResponse.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .applicantName(entity.getApplicantName())
                .dateOfBirth(entity.getDateOfBirth())
                .gender(entity.getGender())
                .applyingForSchoolOrCollege(entity.getApplyingForSchoolOrCollege())
                .parentName(entity.getParentName())
                .guardianName(entity.getGuardianName())
                .contactNumber(entity.getContactNumber())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .city(entity.getCity())
                .state(entity.getState())
                .pinCode(entity.getPincode())
                .emergencyContact(entity.getEmergencyContact())
                .uploadedDocuments(entity.getUploadedDocuments())
                .status(entity.getStatus())
                .internalComments(entity.getInternalComments())
                .build();
    }
}
