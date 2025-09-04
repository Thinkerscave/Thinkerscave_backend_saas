package com.thinkerscave.common.admission.service;

import com.thinkerscave.common.admission.domain.Address;
import com.thinkerscave.common.admission.domain.ApplicationStatus;
import com.thinkerscave.common.admission.domain.EmergencyContact;
import com.thinkerscave.common.admission.dto.*;
import com.thinkerscave.common.admission.domain.ApplicationAdmission;
import com.thinkerscave.common.admission.repository.ApplicationAdmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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
     * @author Bibekananda Pradhan
     * @since 2025-08-05
     */
    @Transactional
    public ApplicationAdmissionResponse create(ApplicationAdmissionCreateRequest request) {
        // --- MODIFICATION START ---
        // Create Address and EmergencyContact objects from the nested DTOs
        Address address = new Address(
                request.getAddress().getStreet(),
                request.getAddress().getCity(),
                request.getAddress().getState(),
                request.getAddress().getPinCode()
        );

        EmergencyContact emergencyContact = new EmergencyContact(
                request.getEmergencyContact().getName(),
                request.getEmergencyContact().getNumber()
        );

        ApplicationAdmission entity = ApplicationAdmission.builder()
                .applicationId(generateUniqueApplicationId())
                .applicantName(request.getApplicantName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .applyingForSchoolOrCollege(request.getApplyingForSchoolOrCollege())
                .parentName(request.getParentName())
                .guardianName(request.getGuardianName())
                .contactNumber(request.getContactNumber())
                .email(request.getEmail())
                .address(address) // Set the embedded Address object
                .emergencyContact(emergencyContact) // Set the embedded EmergencyContact object
                .uploadedDocuments(request.getUploadedDocuments())
                .status(request.getStatus())
                .internalComments(request.getInternalComments())
                .build();
        // --- MODIFICATION END ---

        entity = repository.save(entity);
        return toResponse(entity);
    }

    /**
     * Updates an existing ApplicationAdmission entity with data from the provided DTO.
     *
     * @param id ApplicationAdmission id
     * @param request DTO containing fields to update
     * @return Response DTO with the updated application's details, if found
     * @author Bibekananda Pradhan
     * @since 2025-08-05
     */
    @Transactional
    public Optional<ApplicationAdmissionResponse> edit(String id, ApplicationAdmissionEditRequest request) {
        return repository.findById(id).map(entity -> {
            // --- MODIFICATION START ---
            // Update Address and EmergencyContact objects
            Address address = new Address(
                    request.getAddress().getStreet(),
                    request.getAddress().getCity(),
                    request.getAddress().getState(),
                    request.getAddress().getPinCode()
            );

            EmergencyContact emergencyContact = new EmergencyContact(
                    request.getEmergencyContact().getName(),
                    request.getEmergencyContact().getNumber()
            );

            entity.setApplicantName(request.getApplicantName());
            entity.setDateOfBirth(request.getDateOfBirth());
            entity.setGender(request.getGender());
            entity.setApplyingForSchoolOrCollege(request.getApplyingForSchoolOrCollege());
            entity.setParentName(request.getParentName());
            entity.setGuardianName(request.getGuardianName());
            entity.setContactNumber(request.getContactNumber());
            entity.setEmail(request.getEmail());
            entity.setAddress(address); // Set the updated Address object
            entity.setEmergencyContact(emergencyContact); // Set the updated EmergencyContact object
            entity.setUploadedDocuments(request.getUploadedDocuments());
            entity.setStatus(request.getStatus());
            entity.setInternalComments(request.getInternalComments());
            // --- MODIFICATION END ---

            ApplicationAdmission updated = repository.save(entity);
            return toResponse(updated);
        });
    }

    /**
     * Retrieves an ApplicationAdmission entity by its id and maps it to a response DTO.
     *
     * @param id ApplicationAdmission id
     * @return Response DTO with the application's details, if found
     * @author Bibekananda Pradhan
     * @since 2025-08-05
     */
    @Transactional(readOnly = true)
    public Optional<ApplicationAdmissionResponse> getById(String id) {
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
        // --- MODIFICATION START ---
        // Assuming ApplicationAdmissionResponse also has nested DTOs for Address and EmergencyContact
        AddressDto addressDto = new AddressDto(
                entity.getAddress().getStreet(),
                entity.getAddress().getCity(),
                entity.getAddress().getState(),
                entity.getAddress().getPincode()
        );

        EmergencyContactDto emergencyContactDto = new EmergencyContactDto(
                entity.getEmergencyContact().getName(),
                entity.getEmergencyContact().getNumber()
        );

        return ApplicationAdmissionResponse.builder()
                .applicationId(entity.getApplicationId())
                .applicantName(entity.getApplicantName())
                .dateOfBirth(entity.getDateOfBirth())
                .gender(entity.getGender())
                .applyingForSchoolOrCollege(entity.getApplyingForSchoolOrCollege())
                .parentName(entity.getParentName())
                .guardianName(entity.getGuardianName())
                .contactNumber(entity.getContactNumber())
                .email(entity.getEmail())
                .address(addressDto) // Map to the nested Address DTO
                .emergencyContact(emergencyContactDto) // Map to the nested EmergencyContact DTO
                .uploadedDocuments(entity.getUploadedDocuments())
                .status(entity.getStatus())
                .internalComments(entity.getInternalComments())
                .build();
        // --- MODIFICATION END ---
    }
    @Transactional
    public ApplicationAdmissionResponse saveDraft(ApplicationAdmissionDraftRequest request) {
        // Find an existing draft by its database 'id' or create a new entity if no id is provided.
        ApplicationAdmission entity = Optional.ofNullable(request.getApplicationId())
                .flatMap(repository::findById)
                .orElse(new ApplicationAdmission());

        // --- Map DTO data directly from the flattened request ---
        // This removes the need for checking nested objects like 'basicInfo'
        entity.setApplicantName(request.getApplicantName()) ;
        entity.setDateOfBirth(request.getDateOfBirth());
        entity.setGender(request.getGender());
        entity.setApplyingForSchoolOrCollege(request.getApplyingForSchoolOrCollege());
        entity.setParentName(request.getParentName());
        entity.setGuardianName(request.getGuardianName());
        entity.setEmail(request.getEmail());
        entity.setContactNumber(request.getContactNumber());

        // Address (Embedded) - This mapping remains the same as it's still a nested object
        if (request.getAddress() != null) {
            Address address = new Address(
                    request.getAddress().getStreet(),
                    request.getAddress().getCity(),
                    request.getAddress().getState(),
                    request.getAddress().getPincode()
            );
            entity.setAddress(address);
        }

        // Emergency Contact (Embedded) - This mapping also remains the same
        if (request.getEmergencyContact() != null) {
            EmergencyContact contact = new EmergencyContact(
                    request.getEmergencyContact().getName(),
                    request.getEmergencyContact().getNumber()
            );
            entity.setEmergencyContact(contact);
        }

        // Map the list of document file names
        if (request.getUploadedDocuments() != null) {
            entity.setUploadedDocuments(request.getUploadedDocuments());
        }

        // Set the status to DRAFT
        entity.setStatus(ApplicationStatus.DRAFT);

        // Save the entity (JPA handles create vs. update based on the presence of the 'id')
        ApplicationAdmission savedEntity = repository.save(entity);

        // Map the saved entity to a response DTO
        return toResponse(savedEntity);
    }
    /**
     * Generates a unique application ID.
     * Example: "APP-20250901-A3CDE"
     */
    private static String generateUniqueApplicationId() {
        String timestampPart = String.valueOf(Instant.now().toEpochMilli()); // Milliseconds for uniqueness
        String randomPart = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return "APP-" + timestampPart + "-" + randomPart;
    }
}