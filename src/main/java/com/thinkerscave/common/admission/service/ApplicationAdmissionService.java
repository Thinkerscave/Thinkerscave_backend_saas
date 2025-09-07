package com.thinkerscave.common.admission.service;

import com.thinkerscave.common.admission.domain.AddressEmbedded;
import com.thinkerscave.common.admission.domain.ApplicationStatus;
import com.thinkerscave.common.admission.domain.EmergencyContact;
import com.thinkerscave.common.admission.dto.*;
import com.thinkerscave.common.admission.domain.ApplicationAdmission;
import com.thinkerscave.common.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.common.commonModel.Address;
import com.thinkerscave.common.commonModel.AddressRepository;
import com.thinkerscave.common.student.domain.Guardian;
import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.repository.GuardianRepository;
import com.thinkerscave.common.student.repository.StudentRepository;
import com.thinkerscave.common.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

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
    private final StudentRepository studentRepository;
    private final AddressRepository addressRepository;
    private final GuardianRepository guardianRepository;

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
        AddressEmbedded address = new AddressEmbedded(
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
     * @param id      ApplicationAdmission id
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
            AddressEmbedded address = new AddressEmbedded(
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
        entity.setApplicantName(request.getApplicantName());
        entity.setDateOfBirth(request.getDateOfBirth());
        entity.setGender(request.getGender());
        entity.setApplyingForSchoolOrCollege(request.getApplyingForSchoolOrCollege());
        entity.setParentName(request.getParentName());
        entity.setGuardianName(request.getGuardianName());
        entity.setEmail(request.getEmail());
        entity.setContactNumber(request.getContactNumber());

        // Address (Embedded) - This mapping remains the same as it's still a nested object
        if (request.getAddress() != null) {
            AddressEmbedded address = new AddressEmbedded(
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

    /**
     * Updates the status of application(s) based on the request.
     * - Approves or rejects pending applications.
     * - Creates Student records for approved applications.
     * - Collects invalid application IDs (not found in DB).
     *
     * @param request request containing application IDs and new status
     * @return response object with update summary (updated count, students created, invalid IDs)
     */
    @Transactional
    public ApplicationStatusUpdateResponse updateApplicationStatus(@Valid ApplicationStatusUpdateRequest request) {
        // 1. Fetch all applications matching the provided IDs
        List<ApplicationAdmission> applications = repository.findByApplicationIdIn(request.getApplicationIds());

        // 2. Determine valid and invalid IDs
        Set<String> validApplicationIds = applications.stream()
                .map(ApplicationAdmission::getApplicationId)
                .collect(Collectors.toSet());

        List<String> invalidApplicationIds = request.getApplicationIds().stream()
                .filter(id -> !validApplicationIds.contains(id))
                .toList();

        // 3. Process only applications in PENDING status
        List<ApplicationAdmission> pendingApplications = applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .toList();

        int studentsCreated = 0;

        // 4. Update status and create students if APPROVED
        for (ApplicationAdmission application : pendingApplications) {
            application.setStatus(request.getStatus());

            if (request.getStatus() == ApplicationStatus.APPROVED) {
                createStudentFromApplication(application);
                studentsCreated++;
            }
        }

        // 5. Save updated applications in bulk
        repository.saveAll(pendingApplications);

        // 6. Return response object
        return ApplicationStatusUpdateResponse.builder()
                .updatedCount(pendingApplications.size())
                .studentsCreated(studentsCreated)
                .invalidApplicationIds(invalidApplicationIds)
                .build();
    }

    /**
     * Orchestrates the creation of a Student entity from an approved application.
     * Steps:
     * 1. Create and save Address
     * 2. Create and save Guardian
     * 3. Build Student object and save
     *
     * @param application application details used to create student
     */
    @Transactional
    private void createStudentFromApplication(ApplicationAdmission application) {
        Address savedAddress = createAndSaveAddress(application);
        Guardian savedGuardian = createAndSaveGuardian(application);

        Student student = createStudent(application, savedAddress, savedGuardian);
        studentRepository.save(student);
    }

    /**
     * Creates and persists an Address entity based on the application.
     *
     * @param application source application with address details
     * @return saved Address entity
     */
    private Address createAndSaveAddress(ApplicationAdmission application) {
        Address address = new Address();
        address.setAddressLine(application.getAddress().getStreet());
        address.setCity(application.getAddress().getCity());
        address.setState(application.getAddress().getState());
        address.setZipCode(application.getAddress().getPincode());
        return addressRepository.save(address);
    }

    /**
     * Creates and persists a Guardian entity based on the application.
     *
     * @param application source application with guardian details
     * @return saved Guardian entity
     */
    private Guardian createAndSaveGuardian(ApplicationAdmission application) {
        Guardian guardian = new Guardian();
        guardian.setFirstName(application.getParentName());
        guardian.setLastName(application.getGuardianName());
        guardian.setMobileNumber(Long.parseLong(application.getContactNumber()));
        guardian.setEmail(application.getEmail());
        guardian.setAddress(String.format("%s, %s, %s, %s",
                application.getAddress().getStreet(),
                application.getAddress().getCity(),
                application.getAddress().getState(),
                application.getAddress().getPincode()));
        return guardianRepository.save(guardian);
    }

    /**
     * Builds a Student entity from application, guardian, and address data.
     * Note: This does NOT persist the entity. Callers must save it.
     *
     * @param application   source application
     * @param savedAddress  persisted Address entity
     * @param savedGuardian persisted Guardian entity
     * @return constructed Student entity (not yet saved)
     */
    private Student createStudent(ApplicationAdmission application, Address savedAddress, Guardian savedGuardian) {
        Student student = new Student();

        // Split full name into parts
        String[] nameParts = application.getApplicantName().split(" ", 3);
        student.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
        student.setMiddleName(nameParts.length > 1 ? nameParts[1] : "");
        student.setLastName(nameParts.length > 2 ? nameParts[2] : "");

        student.setParent(savedGuardian);
        student.setEmail(application.getEmail());
        student.setMobileNumber(Long.parseLong(application.getContactNumber()));
        student.setGender(application.getGender());
        student.setDateOfBirth(application.getDateOfBirth().toLocalDate());
        student.setEnrollmentDate(LocalDate.now());
        student.setActive(true);

        // Set addresses (current and permanent same at start)
        student.setCurrentAddress(savedAddress);
        student.setPermanentAddress(savedAddress);
        student.setSameAddress(true);

        // Generate roll number (simple implementation)
        student.setRollNumber(generateRollNumber());

        // Audit fields
        student.setCreatedBy("System");
        student.setCreatedDate(new Date());

        return student;
    }

    /**
     * Generates a roll number for a student.
     * Currently based on timestamp, but should be replaced with
     * sequence/UUID/logic as per business requirement.
     *
     * @return unique roll number string
     */
    private String generateRollNumber() {
        return "STU" + System.currentTimeMillis();
    }
}