package com.thinkerscave.student.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.dto.UserProvisioningResult;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.entity.UserRole;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.repository.UserRoleRepository;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.admission.dto.ApplicationProfileDetails;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.document.entity.Document;
import com.thinkerscave.document.enums.DocumentOwnerType;
import com.thinkerscave.document.repository.DocumentRepository;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.admission.enums.DocumentCheckStatus;
import com.thinkerscave.security.service.UserCredentialNotificationService;
import com.thinkerscave.shared.entity.Address;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.dto.AddressDto;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.service.AddressFactory;
import com.thinkerscave.shared.storage.LocalFileStorageService;
import com.thinkerscave.student.dto.EnrollmentDTO;
import com.thinkerscave.student.dto.ImportedStudentDocument;
import com.thinkerscave.student.dto.MedicalDTO;
import com.thinkerscave.student.dto.ParentDTO;
import com.thinkerscave.student.dto.StudentParentRequest;
import com.thinkerscave.student.dto.PreviousSchoolingDTO;
import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentPersonalUpdateRequest;
import com.thinkerscave.student.dto.StudentParentRequest;
import com.thinkerscave.student.dto.StudentDocumentDTO;
import com.thinkerscave.student.dto.StudentProfileResponse;
import com.thinkerscave.student.dto.StudentResponseDTO;
import com.thinkerscave.student.dto.StudentSearchRequest;
import com.thinkerscave.student.dto.TimelineDTO;
import com.thinkerscave.student.entity.Parent;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.entity.StudentMedical;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.entity.StudentTimeline;
import com.thinkerscave.student.enums.EnrollmentStatus;
import com.thinkerscave.student.enums.ParentRelationship;
import com.thinkerscave.student.enums.StudentStatus;
import com.thinkerscave.student.enums.StudentTimelineEventType;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentMedicalRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.repository.StudentTimelineRepository;
import com.thinkerscave.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final String ROLE_STUDENT = "ROLE_STUDENT";
    private static final String ROLE_PARENT = "ROLE_PARENT";
    private static final String TYPE_STUDENT = "student";
    private static final String TYPE_GUARDIAN = "guardian";

    private final LocalFileStorageService fileStorageService;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentMedicalRepository studentMedicalRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentTimelineRepository studentTimelineRepository;
    private final DocumentRepository documentRepository;
    private final AddressFactory addressFactory;
    private final AuditWriteService auditWriteService;
    private final ApplicationAdmissionRepository applicationAdmissionRepository;
    private final ObjectMapper objectMapper;
    private final UserCredentialNotificationService credentialNotificationService;

    @Override
    @Transactional
    public StudentResponseDTO createStudent(StudentCreateRequest request) throws IOException {
        return saveStudentWithDocuments(request, null, null, null);
    }

    @Override
    @Transactional
    public StudentResponseDTO saveStudentWithDocuments(StudentCreateRequest dto, MultipartFile photo,
                                                       List<MultipartFile> documents, List<String> types) throws IOException {

        validateParentRequests(dto);
        InitialPlacement initialPlacement = validateInitialPlacement(dto);
        UserProvisioningResult studentProvisioning = null;
        User studentUser = null;
        if (hasText(dto.getEmail())) {
            User existingUser = userRepository.findByEmailIgnoreCase(dto.getEmail().trim()).orElse(null);
            if (existingUser != null && studentRepository.findByUser_Id(existingUser.getId()).isPresent()) {
                throw new BadRequestException("This account is already linked to a student");
            }
            Role studentRole = roleRepository.findByRoleCode(ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("Role STUDENT not found"));
            AccountProvisioning account = provisionAccount(dto.getEmail(), new UserCreationContext(
                    dto.getFirstName(), dto.getMiddleName(), dto.getLastName(),
                    dto.getEmail(), dto.getMobileNumber(), null, null, null, TYPE_STUDENT), studentRole);
            studentUser = account.user();
            studentProvisioning = account.newAccount();
        }

        String studentCode = "STU" + System.currentTimeMillis();
        String admissionNumber = trimToNull(dto.getAdmissionNumber());
        if (admissionNumber != null && studentRepository.existsByAdmissionNumber(admissionNumber)) {
            throw new BadRequestException("Admission number already exists: " + admissionNumber);
        }

        Student student = new Student();
        student.setStudentCode(studentCode);
        student.setAdmissionNumber(admissionNumber);
        student.setRollNumber(trimToNull(dto.getRollNumber()));
        student.setFirstName(dto.getFirstName().trim());
        student.setMiddleName(trimToNull(dto.getMiddleName()));
        student.setLastName(dto.getLastName().trim());
        student.setGender(normalizeGender(dto.getGender()));
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setReligion(trimToNull(dto.getReligion()));
        student.setNationality(trimToNull(dto.getNationality()));
        student.setMotherTongue(trimToNull(dto.getMotherTongue()));
        student.setSocialCategory(trimToNull(dto.getSocialCategory() != null ? dto.getSocialCategory() : dto.getCategory()));
        student.setPlaceOfBirth(trimToNull(dto.getPlaceOfBirth()));
        student.setIdentityDocumentType(trimToNull(dto.getIdentityDocumentType()));
        student.setIdentityDocumentNumber(trimToNull(dto.getIdentityDocumentNumber()));
        student.setApplicationId(dto.getApplicationId());
        student.setMobileNumber(parseMobile(dto.getMobileNumber()));
        student.setEmail(trimToNull(dto.getEmail()));
        student.setRemarks(trimToNull(dto.getRemarks()));
        student.setStatus(StudentStatus.ACTIVE);
        student.setAdmissionDate(dto.getEnrollmentDate() != null ? dto.getEnrollmentDate() : java.time.LocalDate.now());
        student.setUser(studentUser);
        student.setEmergencyContactName(trimToNull(dto.getEmergencyContactName()));
        student.setEmergencyContactPhone(trimToNull(dto.getEmergencyContactPhone()));
        student.setEmergencyContactRelation(trimToNull(dto.getEmergencyContactRelation()));

        applyAddresses(student, dto);

        if (photo != null && !photo.isEmpty()) {
            student.setPhotoUrl(fileStorageService.store(photo, "photo_" + safeFileToken(studentCode)));
        }

        student = studentRepository.save(student);

        StudentMedical medical = new StudentMedical();
        medical.setStudent(student);
        medical.setBloodGroup(trimToNull(dto.getBloodGroup()));
        medical.setAllergies(trimToNull(dto.getAllergies()));
        medical.setMedicalConditions(trimToNull(dto.getMedicalConditions()));
        medical.setMedications(trimToNull(dto.getMedications()));
        medical.setDoctorName(trimToNull(dto.getDoctorName()));
        medical.setDoctorContact(trimToNull(dto.getDoctorContact()));
        medical.setEmergencyNotes(trimToNull(dto.getEmergencyNotes()));
        studentMedicalRepository.save(medical);

        saveParentLinks(student, dto);

        if (initialPlacement != null) {
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setStudent(student);
            enrollment.setRollNumber(trimToNull(dto.getRollNumber()));
            enrollment.setStatus(parseEnrollmentStatus(dto.getEnrollmentStatus()));
            enrollment.setActive(true);
            enrollment.setAcademicYear(initialPlacement.academicYear());
            enrollment.setClassEntity(initialPlacement.classEntity());
            enrollment.setSection(initialPlacement.section());
            studentEnrollmentRepository.save(enrollment);
        }

        if (documents != null && types != null) {
            for (int i = 0; i < documents.size(); i++) {
                MultipartFile file = documents.get(i);
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String storedPath = fileStorageService.store(file, "doc_" + safeFileToken(studentCode));
                Document document = new Document();
                document.setOwnerType(DocumentOwnerType.STUDENT);
                document.setOwnerId(student.getStudentId());
                document.setDocumentName(file.getOriginalFilename());
                document.setDocumentType(types.get(i) == null || types.get(i).isBlank() ? "OTHER" : types.get(i));
                document.setFilePath(storedPath);
                document.setFileName(fileStorageService.sanitizeFilename(file.getOriginalFilename()));
                document.setFileExtension(extensionOf(document.getFileName()));
                document.setMimeType(file.getContentType());
                document.setFileSize(file.getSize());
                document.setVerificationStatus(DocumentCheckStatus.PENDING.name());
                document.setUploadedOn(LocalDateTime.now());
                document.setCreatedBy(getCurrentUsername());
                documentRepository.save(document);
            }
        }

        addTimelineEvent(student, StudentTimelineEventType.STUDENT_CREATED,
                "Student Admitted", "Student successfully registered in system.");
        auditWriteService.record(
                AuditEventType.CREATE,
                "STUDENT_CREATE",
                "Student",
                String.valueOf(student.getStudentId()),
                "Student created: " + studentCode);

        if (studentProvisioning != null) {
            credentialNotificationService.sendInstitutionUserSetupInvitation(studentProvisioning.getUser(), "Student");
        }

        return mapToResponseDTO(student);
    }

    @Override
    @Transactional
    public StudentResponseDTO updateStudent(Long studentId, StudentCreateRequest dto) {
        Student student = getStudent(studentId);
        student.setAdmissionNumber(dto.getAdmissionNumber());
        student.setRollNumber(dto.getRollNumber());
        student.setFirstName(dto.getFirstName());
        student.setMiddleName(dto.getMiddleName());
        student.setLastName(dto.getLastName());
        student.setGender(dto.getGender());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setReligion(dto.getReligion());
        student.setNationality(dto.getNationality());
        student.setMotherTongue(dto.getMotherTongue());
        student.setSocialCategory(trimToNull(dto.getSocialCategory() != null ? dto.getSocialCategory() : dto.getCategory()));
        student.setPlaceOfBirth(trimToNull(dto.getPlaceOfBirth()));
        student.setIdentityDocumentType(trimToNull(dto.getIdentityDocumentType()));
        student.setIdentityDocumentNumber(trimToNull(dto.getIdentityDocumentNumber()));
        if (dto.getApplicationId() != null) {
            student.setApplicationId(dto.getApplicationId());
        }
        student.setEmergencyContactName(trimToNull(dto.getEmergencyContactName()));
        student.setEmergencyContactPhone(trimToNull(dto.getEmergencyContactPhone()));
        student.setEmergencyContactRelation(trimToNull(dto.getEmergencyContactRelation()));
        student.setMobileNumber(dto.getMobileNumber() != null ? Long.parseLong(dto.getMobileNumber()) : null);
        student.setEmail(dto.getEmail());
        student.setRemarks(dto.getRemarks());
        applyAddresses(student, dto);
        studentRepository.save(student);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Student Updated", "Student profile details were updated.");
        auditWriteService.record(
                AuditEventType.UPDATE,
                "STUDENT_UPDATE",
                "Student",
                String.valueOf(student.getStudentId()),
                "Student profile updated: " + student.getStudentCode());
        return mapToResponseDTO(student);
    }

    @Override
    public StudentResponseDTO getStudentById(Long studentId) {
        return mapToResponseDTO(getStudent(studentId));
    }

    @Override
    public StudentProfileResponse getProfile360(Long studentId) {
        Student student = getStudent(studentId);

        StudentProfileResponse response = new StudentProfileResponse();
        response.setStudent(mapToResponseDTO(student));

        studentEnrollmentRepository.findActiveWithClassByStudentId(studentId).ifPresent(enrollment -> {
            response.setEnrollment(mapEnrollment(enrollment));
        });

        List<ParentDTO> parents = studentParentRepository.findByStudentStudentId(studentId).stream()
                .map(this::mapParentLink)
                .collect(Collectors.toList());
        response.setParents(parents);
        parents.stream()
                .filter(p -> Boolean.TRUE.equals(p.getPrimaryContact()))
                .findFirst()
                .or(() -> parents.stream().findFirst())
                .ifPresent(response::setParent);

        studentMedicalRepository.findByStudentStudentId(studentId).ifPresent(medical -> {
            MedicalDTO dto = new MedicalDTO();
            dto.setBloodGroup(medical.getBloodGroup());
            dto.setAllergies(medical.getAllergies());
            dto.setMedicalConditions(medical.getMedicalConditions());
            dto.setMedications(medical.getMedications());
            dto.setDoctorName(medical.getDoctorName());
            dto.setDoctorContact(medical.getDoctorContact());
            dto.setEmergencyNotes(medical.getEmergencyNotes());
            response.setMedical(dto);
        });

        documentRepository.findByOwnerTypeAndOwnerIdAndActiveTrueOrderByDocumentIdDesc(DocumentOwnerType.STUDENT, studentId).stream()
                .filter(doc -> "PHOTO".equalsIgnoreCase(doc.getDocumentType()))
                .findFirst()
                .ifPresent(photoDoc -> response.setPhotoDocumentId(photoDoc.getDocumentId()));

        if (student.getApplicationId() != null) {
            applicationAdmissionRepository.findById(student.getApplicationId()).ifPresent(app -> {
                ApplicationProfileDetails profile = readApplicationProfile(app.getProfileDetails());
                if (profile != null && (Boolean.TRUE.equals(profile.getHasPreviousSchooling())
                        || hasText(profile.getPreviousSchoolName()))) {
                    PreviousSchoolingDTO prev = new PreviousSchoolingDTO();
                    prev.setHasPreviousSchooling(Boolean.TRUE.equals(profile.getHasPreviousSchooling())
                            || hasText(profile.getPreviousSchoolName()));
                    prev.setSchoolName(profile.getPreviousSchoolName());
                    prev.setBoard(profile.getPreviousBoard());
                    prev.setClassName(profile.getPreviousClass());
                    prev.setAcademicYear(profile.getPreviousAcademicYear());
                    prev.setPercentage(profile.getLastPercentage());
                    prev.setTcNumber(profile.getTcNumber());
                    prev.setTcDate(profile.getTcDate());
                    response.setPreviousSchooling(prev);
                }
            });
        }

        response.setTimeline(getTimeline(studentId));
        return response;
    }

    @Override
    @Transactional
    public ParentDTO addParent(Long studentId, StudentParentRequest request) {
        Student student = getStudent(studentId);
        if (request == null || !request.hasIdentity()) {
            throw new BadRequestException("Parent first name and mobile are required");
        }
        List<StudentParent> current = studentParentRepository.findByStudentStudentId(studentId);
        Parent parent = resolveOrCreateParent(request);
        ParentRelationship relationship = parseRelationship(request.getRelationship(), ParentRelationship.GUARDIAN);
        StudentParent duplicate = current.stream()
                .filter(link -> link.getParent().getParentId().equals(parent.getParentId())
                        && link.getRelationship() == relationship && Boolean.TRUE.equals(link.getActive()))
                .findFirst().orElse(null);
        if (duplicate != null) {
            throw new BadRequestException("This parent is already linked to the student");
        }

        boolean makePrimary = Boolean.TRUE.equals(request.getPrimaryContact())
                || current.stream().noneMatch(link -> Boolean.TRUE.equals(link.getPrimaryContact())
                        && Boolean.TRUE.equals(link.getActive()));
        if (makePrimary) {
            current.forEach(link -> {
                link.setPrimaryContact(false);
                studentParentRepository.save(link);
            });
        }
        StudentParent link = new StudentParent();
        link.setStudent(student);
        link.setParent(parent);
        link.setRelationship(relationship);
        link.setPrimaryContact(makePrimary);
        link.setReceiveSms(request.getReceiveSms() == null || request.getReceiveSms());
        link.setReceiveEmail(request.getReceiveEmail() == null || request.getReceiveEmail());
        link.setPickupAuthorized(Boolean.TRUE.equals(request.getPickupAuthorized()));
        link.setActive(true);
        studentParentRepository.save(link);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED, "Guardian added",
                "A parent or guardian was linked to the student.");
        return mapParentLink(link);
    }

    @Override
    @Transactional
    public ParentDTO updateParent(Long studentId, Long parentId, StudentParentRequest request) {
        Student student = getStudent(studentId);
        StudentParent link = studentParentRepository.findByStudentStudentId(studentId).stream()
                .filter(candidate -> candidate.getParent().getParentId().equals(parentId)
                        && Boolean.TRUE.equals(candidate.getActive()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Guardian link not found for student"));
        Parent parent = link.getParent();
        parent.setFirstName(request.getFirstName().trim());
        parent.setMiddleName(trimToNull(request.getMiddleName()));
        parent.setLastName(hasText(request.getLastName()) ? request.getLastName().trim() : "NA");
        parent.setMobileNumber(request.getMobileNumber().trim());
        parent.setOccupation(trimToNull(request.getOccupation()));
        parent.setOrganizationName(trimToNull(request.getOrganizationName()));
        parent.setQualification(trimToNull(request.getQualification()));
        parent.setAnnualIncome(request.getAnnualIncome());
        if (!java.util.Objects.equals(parent.getEmail(), trimToNull(request.getEmail()))) {
            String nextEmail = trimToNull(request.getEmail());
            if (nextEmail != null && userRepository.existsByEmail(nextEmail)) {
                boolean sameLinkedAccount = parent.getUser() != null
                        && parent.getUser().getEmail().equalsIgnoreCase(nextEmail);
                if (!sameLinkedAccount) throw new BadRequestException("Email is already used by another account");
            }
            parent.setEmail(nextEmail);
            if (parent.getUser() != null && nextEmail != null) {
                parent.getUser().setEmail(nextEmail);
                parent.getUser().setUsername(nextEmail);
                parent.getUser().setEmailVerified(false);
                userRepository.save(parent.getUser());
                credentialNotificationService.sendInstitutionUserSetupInvitation(parent.getUser(), "Parent / Guardian");
            }
        }
        parentRepository.save(parent);
        link.setRelationship(parseRelationship(request.getRelationship(), link.getRelationship()));
        if (Boolean.TRUE.equals(request.getPrimaryContact())) {
            studentParentRepository.findByStudentStudentId(studentId).stream()
                    .filter(candidate -> !candidate.getStudentParentId().equals(link.getStudentParentId()))
                    .forEach(candidate -> {
                        candidate.setPrimaryContact(false);
                        studentParentRepository.save(candidate);
                    });
            link.setPrimaryContact(true);
        }
        if (request.getReceiveSms() != null) link.setReceiveSms(request.getReceiveSms());
        if (request.getReceiveEmail() != null) link.setReceiveEmail(request.getReceiveEmail());
        if (request.getPickupAuthorized() != null) link.setPickupAuthorized(request.getPickupAuthorized());
        studentParentRepository.save(link);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED, "Guardian updated",
                "A parent or guardian profile was updated.");
        return mapParentLink(link);
    }

    @Override
    @Transactional
    public void removeParent(Long studentId, Long parentId) {
        Student student = getStudent(studentId);
        List<StudentParent> links = studentParentRepository.findByStudentStudentId(studentId).stream()
                .filter(link -> Boolean.TRUE.equals(link.getActive())).toList();
        StudentParent remove = links.stream()
                .filter(link -> link.getParent().getParentId().equals(parentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Guardian link not found for student"));
        if (links.size() <= 1) {
            throw new BadRequestException("A student must retain at least one linked parent or guardian");
        }
        boolean wasPrimary = Boolean.TRUE.equals(remove.getPrimaryContact());
        remove.setActive(false);
        remove.setPrimaryContact(false);
        studentParentRepository.save(remove);
        if (wasPrimary) {
            links.stream().filter(link -> !link.getStudentParentId().equals(remove.getStudentParentId()))
                    .findFirst().ifPresent(link -> {
                        link.setPrimaryContact(true);
                        studentParentRepository.save(link);
                    });
        }
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED, "Guardian removed",
                "A parent or guardian was unlinked from the student.");
    }

    @Override
    @Transactional
    public StudentResponseDTO updatePersonal(Long studentId, StudentPersonalUpdateRequest dto) {
        Student student = getStudent(studentId);
        student.setFirstName(dto.getFirstName().trim());
        student.setLastName(dto.getLastName().trim());
        if (dto.getGender() != null) student.setGender(trimToNull(dto.getGender()));
        if (dto.getDateOfBirth() != null) student.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getMobile() != null) {
            student.setMobileNumber(parseMobile(dto.getMobile()));
        }
        if (dto.getEmail() != null) student.setEmail(trimToNull(dto.getEmail()));
        updatePersonalAddresses(student, dto);
        studentRepository.save(student);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Profile Updated", "Personal information was updated.");
        auditWriteService.record(
                AuditEventType.UPDATE,
                "STUDENT_PERSONAL_UPDATE",
                "Student",
                String.valueOf(student.getStudentId()),
                "Student personal information updated: " + student.getStudentCode());
        return mapToResponseDTO(student);
    }

    private void updatePersonalAddresses(Student student, StudentPersonalUpdateRequest request) {
        updateAddress(student.getCurrentAddress(), request.getCurrentAddressLine1(), request.getCurrentAddressLine2(),
                request.getCurrentLandmark(), request.getCurrentCity(), request.getCurrentDistrict(),
                request.getCurrentState(), request.getCurrentCountry(), request.getCurrentPostalCode(),
                student::setCurrentAddress);
        if (request.getSameAddress() != null) student.setSameAddress(request.getSameAddress());
        if (Boolean.TRUE.equals(request.getSameAddress())) {
            student.setPermanentAddress(copyAddress(student.getCurrentAddress()));
            return;
        }
        updateAddress(student.getPermanentAddress(), request.getPermanentAddressLine1(), request.getPermanentAddressLine2(),
                request.getPermanentLandmark(), request.getPermanentCity(), request.getPermanentDistrict(),
                request.getPermanentState(), request.getPermanentCountry(), request.getPermanentPostalCode(),
                student::setPermanentAddress);
    }

    private void updateAddress(Address existing, String line1, String line2, String landmark, String city,
                               String district, String state, String country, String postal,
                               java.util.function.Consumer<Address> setter) {
        if (existing == null) {
            Address created = buildAddress(line1, line2, landmark, city, district, state, country, postal);
            if (created != null) setter.accept(created);
            return;
        }
        if (line1 != null && hasText(line1)) existing.setAddressLine1(line1.trim());
        if (line2 != null) existing.setAddressLine2(trimToNull(line2));
        if (landmark != null) existing.setLandmark(trimToNull(landmark));
        if (city != null) existing.setCity(trimToNull(city));
        if (district != null) existing.setDistrict(trimToNull(district));
        if (state != null) existing.setState(trimToNull(state));
        if (country != null) existing.setCountry(trimToNull(country));
        if (postal != null) existing.setPostalCode(trimToNull(postal));
    }

    private InitialPlacement validateInitialPlacement(StudentCreateRequest request) {
        if (request.getAcademicYearId() == null && request.getClassId() == null && request.getSectionId() == null) {
            return null;
        }
        if (request.getAcademicYearId() == null || request.getClassId() == null) {
            throw new BadRequestException("Academic year and class are both required for enrollment");
        }
        AcademicYear year = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + request.getAcademicYearId()));
        AcademicClass academicClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));
        if (!academicClass.isActive()) {
            throw new BadRequestException("Cannot enroll a student in an inactive class");
        }
        if (academicClass.getAcademicYear() == null
                || !academicClass.getAcademicYear().getAcademicYearId().equals(year.getAcademicYearId())) {
            throw new BadRequestException("Class does not belong to the selected academic year");
        }
        AcademicSection section = request.getSectionId() == null ? null : sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.getSectionId()));
        if (section != null && (section.getAcademicGroup() == null
                || !section.getAcademicGroup().getAcademicGroupId().equals(academicClass.getClassId()))) {
            throw new BadRequestException("Section does not belong to the selected class");
        }
        return new InitialPlacement(year, academicClass, section);
    }

    private record InitialPlacement(AcademicYear academicYear, AcademicClass classEntity, AcademicSection section) {}

    @Override
    @Transactional
    public StudentResponseDTO updateMedical(Long studentId, MedicalDTO dto) {
        Student student = getStudent(studentId);
        StudentMedical medical = studentMedicalRepository.findByStudentStudentId(studentId)
                .orElseGet(StudentMedical::new);
        medical.setStudent(student);
        medical.setBloodGroup(dto.getBloodGroup());
        medical.setAllergies(dto.getAllergies());
        medical.setMedicalConditions(dto.getMedicalConditions());
        medical.setMedications(dto.getMedications());
        medical.setDoctorName(dto.getDoctorName());
        medical.setDoctorContact(dto.getDoctorContact());
        medical.setEmergencyNotes(dto.getEmergencyNotes());
        studentMedicalRepository.save(medical);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Medical Updated", "Medical details were updated.");
        return mapToResponseDTO(student);
    }

    @Override
    public List<TimelineDTO> getTimeline(Long studentId) {
        getStudent(studentId);
        return studentTimelineRepository.findByStudentStudentIdOrderByCreatedOnDesc(studentId)
                .stream()
                .map(this::mapTimeline)
                .collect(Collectors.toList());
    }

    @Override
    public Page<StudentResponseDTO> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable).map(this::mapToResponseDTO);
    }

    @Override
    public Page<StudentResponseDTO> searchStudents(StudentSearchRequest request, Pageable pageable) {
        Pageable effectivePageable = (pageable == null || pageable.isUnpaged())
                ? Pageable.ofSize(50)
                : pageable;

        String keyword = request == null ? null : trimToNull(request.getKeyword());
        String rollNumber = request == null ? null : trimToNull(request.getRollNumber());
        String parentMobile = request == null ? null : trimToNull(request.getParentMobile());
        String parentName = request == null ? null : trimToNull(request.getParentName());
        return studentRepository.searchDirectory(
                keyword,
                rollNumber,
                parentMobile,
                parentName,
                request == null ? null : request.getAcademicYearId(),
                request == null ? null : request.getClassId(),
                request == null ? null : request.getSectionId(),
                request == null ? null : request.getAssigned(),
                request == null ? null : request.getStatus(),
                effectivePageable)
            .map(this::mapToResponseDTO);
    }

    @Override
    public List<StudentResponseDTO> getAllStudents() {
        // Cap unbounded list endpoint to avoid loading entire tenant table into memory.
        // Prefer getAllStudents(Pageable) / searchStudents for large datasets.
        return studentRepository.findAll(Pageable.ofSize(1000)).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = getStudent(studentId);
        student.setStatus(StudentStatus.INACTIVE);
        studentRepository.save(student);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Student Deactivated", "Student status changed to inactive.");
        auditWriteService.record(
                AuditEventType.STATE_CHANGE,
                "STUDENT_DEACTIVATE",
                "Student",
                String.valueOf(student.getStudentId()),
                "Student deactivated: " + student.getStudentCode());
    }

    @Override
    @Transactional
    public StudentResponseDTO updateStudentStatus(Long studentId, StudentStatus status) {
        Student student = getStudent(studentId);
        if (status != StudentStatus.ACTIVE && status != StudentStatus.INACTIVE) {
            throw new BadRequestException("Student status can only be toggled between ACTIVE and INACTIVE here");
        }
        if (student.getStatus() != StudentStatus.ACTIVE && student.getStatus() != StudentStatus.INACTIVE) {
            throw new BadRequestException("Terminal student lifecycle status can only be changed by its workflow");
        }
        student.setStatus(status);
        studentRepository.save(student);

        StudentTimelineEventType timelineEvent = status == StudentStatus.ALUMNI
                ? StudentTimelineEventType.ALUMNI
                : StudentTimelineEventType.STUDENT_UPDATED;
        addTimelineEvent(student, timelineEvent,
                "Status Updated", "Student status updated to " + status.name() + ".");
        auditWriteService.record(
                AuditEventType.STATE_CHANGE,
                "STUDENT_STATUS_CHANGE",
                "Student",
                String.valueOf(student.getStudentId()),
                "Student status changed to " + status.name() + ": " + student.getStudentCode());

        return mapToResponseDTO(student);
    }

    @Override
    @Transactional
    public TimelineDTO addTimelineEntry(Long studentId, TimelineDTO timelineDTO) {
        Student student = getStudent(studentId);
        StudentTimeline timeline = new StudentTimeline();
        timeline.setStudent(student);
        timeline.setEventType(timelineDTO.getEventType() == null
                ? StudentTimelineEventType.STUDENT_UPDATED
                : StudentTimelineEventType.valueOf(timelineDTO.getEventType()));
        timeline.setTitle(timelineDTO.getTitle());
        timeline.setDescription(timelineDTO.getDescription());
        StudentTimeline saved = studentTimelineRepository.save(timeline);
        return mapTimeline(saved);
    }

    @Override
    public List<StudentDocumentDTO> getStudentDocuments(Long studentId) {
        getStudent(studentId);
        return documentRepository.findByOwnerTypeAndOwnerIdAndActiveTrueOrderByDocumentIdDesc(
                DocumentOwnerType.STUDENT, studentId)
                .stream()
                .map(this::mapDocument)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentDocumentDTO uploadStudentDocument(Long studentId, MultipartFile file, String documentType,
            String documentName, String remarks) throws IOException {
        Student student = getStudent(studentId);
        fileStorageService.validate(file);
        String storedPath = fileStorageService.store(file, "doc_" + safeFileToken(student.getStudentCode()));

        Document document = new Document();
        document.setOwnerType(DocumentOwnerType.STUDENT);
        document.setOwnerId(studentId);
        document.setDocumentName(hasText(documentName) ? documentName.trim() : file.getOriginalFilename());
        document.setDocumentType(documentType == null || documentType.isBlank() ? "OTHER" : documentType);
        document.setFilePath(storedPath);
        document.setFileName(fileStorageService.sanitizeFilename(file.getOriginalFilename()));
        document.setFileExtension(extensionOf(document.getFileName()));
        document.setMimeType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setRemarks(trimToNull(remarks));
        document.setVerificationStatus(DocumentCheckStatus.PENDING.name());
        document.setActive(true);
        document.setCreatedBy(getCurrentUsername());
        document.setUploadedOn(LocalDateTime.now());

        Document saved = documentRepository.save(document);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Document Uploaded", "A student document was uploaded.");
        return mapDocument(saved);
    }

    @Override
    @Transactional
    public StudentDocumentDTO replaceStudentDocument(Long studentId, Long documentId, MultipartFile file,
            String documentName, String remarks) throws IOException {
        Student student = getStudent(studentId);
        fileStorageService.validate(file);
        Document previous = documentRepository.findById(documentId)
            .filter(document -> document.getOwnerType() == DocumentOwnerType.STUDENT
                && document.getOwnerId().equals(studentId)
                        && Boolean.TRUE.equals(document.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        String path = fileStorageService.store(file, "doc_" + safeFileToken(student.getStudentCode()));
        previous.setActive(false);
        Document replacement = new Document();
        replacement.setOwnerType(DocumentOwnerType.STUDENT);
        replacement.setOwnerId(studentId);
        replacement.setDocumentName(hasText(documentName) ? documentName.trim() : previous.getDocumentName());
        replacement.setDocumentType(previous.getDocumentType());
        replacement.setFilePath(path);
        replacement.setFileName(fileStorageService.sanitizeFilename(file.getOriginalFilename()));
        replacement.setFileExtension(extensionOf(replacement.getFileName()));
        replacement.setMimeType(file.getContentType());
        replacement.setFileSize(file.getSize());
        replacement.setVerificationStatus(DocumentCheckStatus.PENDING.name());
        replacement.setRemarks(trimToNull(remarks));
        replacement.setSourceApplicationDocumentId(previous.getSourceApplicationDocumentId());
        replacement.setReplacedDocumentId(previous.getDocumentId());
        replacement.setActive(true);
        replacement.setCreatedBy(getCurrentUsername());
        replacement.setUploadedOn(LocalDateTime.now());
        documentRepository.save(previous);
        return mapDocument(documentRepository.save(replacement));
    }

    @Override
    @Transactional
    public StudentDocumentDTO updateDocumentStatus(Long documentId, String status, String remarks) {
        Document document = documentRepository.findById(documentId)
            .filter(item -> item.getOwnerType() == DocumentOwnerType.STUDENT)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        DocumentCheckStatus checkStatus;
        try {
            checkStatus = DocumentCheckStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid document status: " + status);
        }
        document.setVerificationStatus(checkStatus.name());
        if (remarks != null && !remarks.isBlank()) {
            document.setRemarks(remarks.trim());
        }
        Document saved = documentRepository.save(document);
        Student student = getStudent(document.getOwnerId());
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Document " + checkStatus.name(),
                "Document status updated to " + checkStatus.name() + ".");
        return mapDocument(saved);
    }

    @Override
    @Transactional
    public void importStoredDocuments(Long studentId, List<ImportedStudentDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        Student student = getStudent(studentId);
        for (ImportedStudentDocument item : documents) {
            if (item == null || item.storedPath() == null || item.storedPath().isBlank()) {
                continue;
            }
            Document document = new Document();
            document.setOwnerType(DocumentOwnerType.STUDENT);
            document.setOwnerId(studentId);
            document.setDocumentName(hasText(item.documentName()) ? item.documentName() : "document");
            document.setDocumentType(hasText(item.documentType()) ? item.documentType() : "OTHER");
            document.setFilePath(item.storedPath());
            document.setFileName(fileStorageService.sanitizeFilename(item.documentName()));
            document.setFileExtension(extensionOf(document.getFileName()));
            document.setVerificationStatus(item.status() != null ? item.status().name() : DocumentCheckStatus.PENDING.name());
            document.setSourceApplicationDocumentId(item.sourceApplicationDocumentId());
            if (item.sourceApplicationDocumentId() != null) {
                document.setSourceDocumentType("ADMISSION");
                document.setSourceDocumentId(item.sourceApplicationDocumentId());
            }
            document.setRemarks(trimToNull(item.remarks()));
            document.setCreatedBy(getCurrentUsername());
            document.setActive(true);
            document.setUploadedOn(LocalDateTime.now());
            documentRepository.save(document);
        }
        syncPhotoFromDocuments(studentId);
    }

    @Override
    @Transactional
    public void syncPhotoFromDocuments(Long studentId) {
        Student student = getStudent(studentId);
        if (hasText(student.getPhotoUrl())) {
            return;
        }
        documentRepository.findByOwnerTypeAndOwnerIdAndActiveTrueOrderByDocumentIdDesc(
                DocumentOwnerType.STUDENT, studentId).stream()
                .filter(doc -> "PHOTO".equalsIgnoreCase(doc.getDocumentType()) && hasText(doc.getFilePath()))
                .findFirst()
                .ifPresent(photoDoc -> {
                    student.setPhotoUrl(photoDoc.getFilePath());
                    studentRepository.save(student);
                });
    }

    @Override
    @Transactional
    public void deleteDocument(Long docId) {
        Document document = documentRepository.findById(docId)
            .filter(item -> item.getOwnerType() == DocumentOwnerType.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + docId));
        document.setActive(false);
        documentRepository.save(document);
    }

    @Override
    public Resource downloadDocument(Long docId) {
        Document document = documentRepository.findById(docId)
            .filter(item -> item.getOwnerType() == DocumentOwnerType.STUDENT
                && Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + docId));
        return fileStorageService.loadAsResource(document.getFilePath());
    }

    private void addTimelineEvent(Student student, StudentTimelineEventType type, String title, String description) {
        StudentTimeline timeline = new StudentTimeline();
        timeline.setStudent(student);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timeline.setEventType(type);
        studentTimelineRepository.save(timeline);
    }

    private Student getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
    }

    private java.util.Optional<StudentParent> resolvePrimaryParent(Long studentId) {
        List<StudentParent> links = studentParentRepository.findByStudentStudentId(studentId);
        return links.stream().filter(StudentParent::getPrimaryContact).findFirst()
                .or(() -> links.stream().findFirst());
    }

    private Parent resolveOrCreateParent(StudentParentRequest request) {
        Parent parent = null;
        if (request.getExistingParentId() != null) {
            parent = parentRepository.findById(request.getExistingParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent not found: " + request.getExistingParentId()));
        } else {
            String mobile = trimToNull(request.getMobileNumber());
            if (mobile != null) {
                parent = parentRepository.findByMobileNumber(mobile).orElse(null);
            }
            if (parent == null && hasText(request.getEmail())) {
                parent = parentRepository.findFirstByEmailIgnoreCase(request.getEmail().trim()).orElse(null);
            }
        }

        if (parent == null) {
            parent = new Parent();
            parent.setParentCode("PAR" + System.currentTimeMillis());
            parent.setFirstName(request.getFirstName().trim());
            parent.setMiddleName(trimToNull(request.getMiddleName()));
            parent.setLastName(hasText(request.getLastName()) ? request.getLastName().trim() : "NA");
            parent.setMobileNumber(trimToNull(request.getMobileNumber()));
            parent.setEmail(trimToNull(request.getEmail()));
            parent.setOccupation(trimToNull(request.getOccupation()));
            parent.setOrganizationName(trimToNull(request.getOrganizationName()));
            parent.setQualification(trimToNull(request.getQualification()));
            parent.setAnnualIncome(request.getAnnualIncome());
            parent = parentRepository.save(parent);
        }

        if (!hasText(request.getEmail()) || parent.getUser() != null) {
            return parent;
        }

        Role parentRole = roleRepository.findByRoleCode(ROLE_PARENT)
                .orElseThrow(() -> new IllegalStateException("Role PARENT not found"));
        UserCreationContext guardianContext = new UserCreationContext(
                parent.getFirstName(), parent.getMiddleName(), parent.getLastName(),
                request.getEmail(), request.getMobileNumber(), null, null, null, TYPE_GUARDIAN);
        AccountProvisioning account = provisionAccount(request.getEmail(), guardianContext, parentRole);
        parent.setUser(account.user());
        if (!hasText(parent.getEmail())) parent.setEmail(request.getEmail().trim());
        Parent saved = parentRepository.save(parent);
        if (account.newAccount() != null) {
            credentialNotificationService.sendInstitutionUserSetupInvitation(
                    account.newAccount().getUser(), "Parent / Guardian");
        }
        return saved;
    }

    private AccountProvisioning provisionAccount(String email, UserCreationContext context, Role role) {
        User existing = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (existing == null) {
            UserProvisioningResult created = userService.createUserWithTemporaryPassword(context, role);
            return new AccountProvisioning(created.getUser(), created);
        }
        Long organizationId = OrganizationContext.getOrganizationId();
        if (organizationId != null && !organizationId.equals(existing.getOrganizationId())) {
            throw new BadRequestException("Account belongs to a different organization");
        }
        if (!userRoleRepository.existsByUser_IdAndRole_IdAndActiveTrue(existing.getId(), role.getId())) {
            userRoleRepository.save(UserRole.builder()
                    .user(existing)
                    .role(role)
                    .primaryRole(false)
                    .active(true)
                    .build());
        }
        return new AccountProvisioning(existing, null);
    }

    private record AccountProvisioning(User user, UserProvisioningResult newAccount) {}

    private String buildFullName(String firstName, String middleName, String lastName) {
        return java.util.stream.Stream.of(firstName, middleName, lastName)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    private TimelineDTO mapTimeline(StudentTimeline timeline) {
        TimelineDTO dto = new TimelineDTO();
        dto.setTimelineId(timeline.getTimelineId());
        dto.setTitle(timeline.getTitle());
        dto.setDescription(timeline.getDescription());
        dto.setEventType(timeline.getEventType() != null ? timeline.getEventType().name() : null);
        dto.setCreatedBy(timeline.getCreatedBy());
        dto.setCreatedDate(timeline.getCreatedOn() != null ? timeline.getCreatedOn().toInstant(ZoneOffset.UTC) : null);
        return dto;
    }

    private StudentDocumentDTO mapDocument(Document document) {
        StudentDocumentDTO dto = new StudentDocumentDTO();
        dto.setDocumentId(document.getDocumentId());
        dto.setDocumentName(document.getDocumentName());
        dto.setDocumentType(document.getDocumentType());
        dto.setFileName(document.getFileName());
        dto.setFileExtension(document.getFileExtension());
        dto.setMimeType(document.getMimeType());
        dto.setFileSize(document.getFileSize());
        var uploadedOn = document.getUploadedOn() != null ? document.getUploadedOn() : document.getCreatedOn();
        dto.setUploadedOn(uploadedOn != null ? uploadedOn.toString() : null);
        dto.setSourceApplicationDocumentId(document.getSourceApplicationDocumentId());
        dto.setReplacedDocumentId(document.getReplacedDocumentId());
        dto.setActive(document.getActive());
        DocumentCheckStatus status = DocumentCheckStatus.PENDING;
        try {
            if (hasText(document.getVerificationStatus())) {
                status = DocumentCheckStatus.valueOf(document.getVerificationStatus().trim().toUpperCase(Locale.ROOT));
            }
        } catch (IllegalArgumentException ignored) {
            log.warn("Unknown document verification status for document {}", document.getDocumentId());
        }
        dto.setStatus(status);
        dto.setRemarks(document.getRemarks());
        dto.setDisplayLabel(resolveDocumentDisplayLabel(document.getDocumentType(), document.getRemarks()));
        return dto;
    }

    private String resolveDocumentDisplayLabel(String documentType, String remarks) {
        if (hasText(remarks)) {
            return remarks.trim();
        }
        if (!hasText(documentType)) {
            return "Document";
        }
        String type = documentType.trim();
        if ("OTHER".equalsIgnoreCase(type)) {
            return "Additional document";
        }
        return Arrays.stream(type.split("[_\\s-]+"))
                .filter(part -> !part.isBlank())
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT)
                        + part.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    private ApplicationProfileDetails readApplicationProfile(String json) {
        if (!hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ApplicationProfileDetails.class);
        } catch (Exception ex) {
            log.warn("Could not parse application profile details: {}", ex.getMessage());
            return null;
        }
    }

    private void applyAddresses(Student student, StudentCreateRequest dto) {
        boolean same = Boolean.TRUE.equals(dto.getSameAddress());
        student.setSameAddress(same);
        Address current = buildAddress(
                dto.getCurrentAddressLine1(),
                dto.getCurrentAddressLine2(),
            dto.getCurrentLandmark(),
                dto.getCurrentCity(),
            dto.getCurrentDistrict(),
                dto.getCurrentState(),
                dto.getCurrentCountry(),
                dto.getCurrentPostalCode());
        if (current != null) {
            student.setCurrentAddress(current);
        }
        if (same && current != null) {
            student.setPermanentAddress(copyAddress(current));
            return;
        }
        Address permanent = buildAddress(
                dto.getPermanentAddressLine1(),
                dto.getPermanentAddressLine2(),
            dto.getPermanentLandmark(),
                dto.getPermanentCity(),
            dto.getPermanentDistrict(),
                dto.getPermanentState(),
                dto.getPermanentCountry(),
                dto.getPermanentPostalCode());
        if (permanent != null) {
            student.setPermanentAddress(permanent);
        }
    }

    private Address buildAddress(String line1, String line2, String landmark, String city, String district,
                                 String state, String country, String postal) {
        return addressFactory.build(AddressDto.builder()
                .addressLine1(line1)
                .addressLine2(line2)
                .landmark(landmark)
                .city(city)
                .district(district)
                .state(state)
                .country(country)
                .postalCode(postal)
                .build());
    }

    private Address copyAddress(Address source) {
        return addressFactory.copy(source);
    }

    private void validateParentRequests(StudentCreateRequest request) {
        List<StudentParentRequest> parents = effectiveParents(request);
        long primaryCount = parents.stream().filter(p -> Boolean.TRUE.equals(p.getPrimaryContact())).count();
        if (parents.isEmpty() || primaryCount != 1) {
            throw new BadRequestException("Exactly one primary parent or guardian is required");
        }
        if (parents.stream().anyMatch(p -> !p.hasIdentity())) {
            throw new BadRequestException("Each new parent or guardian requires a first name and mobile number");
        }
    }

    private void saveParentLinks(Student student, StudentCreateRequest request) {
        for (StudentParentRequest parentRequest : effectiveParents(request)) {
            Parent parent = resolveOrCreateParent(parentRequest);
            StudentParent link = new StudentParent();
            link.setStudent(student);
            link.setParent(parent);
            link.setRelationship(parseRelationship(parentRequest.getRelationship(), ParentRelationship.GUARDIAN));
            link.setPrimaryContact(Boolean.TRUE.equals(parentRequest.getPrimaryContact()));
            link.setReceiveSms(parentRequest.getReceiveSms() == null || parentRequest.getReceiveSms());
            link.setReceiveEmail(parentRequest.getReceiveEmail() == null || parentRequest.getReceiveEmail());
            link.setPickupAuthorized(Boolean.TRUE.equals(parentRequest.getPickupAuthorized()));
            link.setActive(true);
            studentParentRepository.save(link);
        }
    }

    private List<StudentParentRequest> effectiveParents(StudentCreateRequest request) {
        if (request.getParents() != null && !request.getParents().isEmpty()) {
            return request.getParents();
        }
        List<StudentParentRequest> parents = new ArrayList<>();
        StudentParentRequest primary = new StudentParentRequest();
        primary.setExistingParentId(request.getExistingParentId());
        primary.setRelationship(request.getParentRelationship());
        primary.setFirstName(request.getParentFirstName());
        primary.setMiddleName(request.getParentMiddleName());
        primary.setLastName(request.getParentLastName());
        primary.setMobileNumber(request.getParentMobileNumber());
        primary.setEmail(request.getParentEmail());
        primary.setOccupation(request.getParentOccupation());
        primary.setOrganizationName(request.getParentOrganizationName());
        primary.setQualification(request.getParentQualification());
        primary.setAnnualIncome(request.getAnnualIncome());
        primary.setPrimaryContact(true);
        parents.add(primary);

        if (hasText(request.getSecondaryParentFirstName()) && hasText(request.getSecondaryParentMobileNumber())) {
            StudentParentRequest secondary = new StudentParentRequest();
            secondary.setRelationship(request.getSecondaryParentRelationship());
            secondary.setFirstName(request.getSecondaryParentFirstName());
            secondary.setLastName(request.getSecondaryParentLastName());
            secondary.setMobileNumber(request.getSecondaryParentMobileNumber());
            secondary.setEmail(request.getSecondaryParentEmail());
            secondary.setOccupation(request.getSecondaryParentOccupation());
            secondary.setPrimaryContact(false);
            parents.add(secondary);
        }
        return parents;
    }

    private ParentRelationship parseRelationship(String raw, ParentRelationship fallback) {
        if (!hasText(raw)) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return ParentRelationship.valueOf(normalized);
        } catch (Exception ex) {
            if (normalized.contains("MOTHER")) return ParentRelationship.MOTHER;
            if (normalized.contains("FATHER")) return ParentRelationship.FATHER;
            if (normalized.contains("GUARDIAN")) return ParentRelationship.GUARDIAN;
            return fallback;
        }
    }

    private EnrollmentStatus parseEnrollmentStatus(String raw) {
        if (!hasText(raw)) {
            return EnrollmentStatus.ACTIVE;
        }
        try {
            return EnrollmentStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return EnrollmentStatus.ACTIVE;
        }
    }

    private String normalizeGender(String gender) {
        if (!hasText(gender)) {
            return "OTHER";
        }
        String g = gender.trim().toUpperCase(Locale.ROOT);
        if (g.startsWith("M")) return "MALE";
        if (g.startsWith("F")) return "FEMALE";
        if ("OTHER".equals(g) || "O".equals(g)) return "OTHER";
        return g;
    }

    private Long parseMobile(String mobile) {
        if (!hasText(mobile)) {
            return null;
        }
        String digits = mobile.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private StudentResponseDTO mapToResponseDTO(Student student) {
        StudentResponseDTO dto = new StudentResponseDTO();
        dto.setStudentId(student.getStudentId());
        dto.setStudentCode(student.getStudentCode());
        dto.setAdmissionNumber(student.getAdmissionNumber());
        dto.setFullName(buildFullName(student.getFirstName(), student.getMiddleName(), student.getLastName()));
        dto.setFirstName(student.getFirstName());
        dto.setMiddleName(student.getMiddleName());
        dto.setLastName(student.getLastName());
        dto.setGender(student.getGender());
        dto.setDateOfBirth(student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : null);
        if (student.getDateOfBirth() != null) {
            dto.setAgeYears(Period.between(student.getDateOfBirth(), LocalDate.now()).getYears());
        }
        dto.setReligion(student.getReligion());
        dto.setNationality(student.getNationality());
        dto.setMotherTongue(student.getMotherTongue());
        dto.setSocialCategory(student.getSocialCategory());
        dto.setCategory(student.getSocialCategory());
        dto.setPlaceOfBirth(student.getPlaceOfBirth());
        dto.setIdentityDocumentType(student.getIdentityDocumentType());
        dto.setIdentityDocumentNumber(student.getIdentityDocumentNumber());
        dto.setApplicationId(student.getApplicationId());
        dto.setMobileNumber(student.getMobileNumber() != null ? student.getMobileNumber().toString() : "");
        dto.setEmail(student.getEmail());
        dto.setStatus(student.getStatus() != null ? student.getStatus().name() : StudentStatus.ACTIVE.name());
        dto.setSameAddress(student.getSameAddress());
        dto.setEmergencyContactName(student.getEmergencyContactName());
        dto.setEmergencyContactPhone(student.getEmergencyContactPhone());
        dto.setEmergencyContactRelation(student.getEmergencyContactRelation());
        dto.setRollNumber(student.getRollNumber());
        dto.setAdmissionDate(student.getAdmissionDate() != null ? student.getAdmissionDate().toString() : null);
        dto.setPhotoUrl(student.getPhotoUrl());
        dto.setRemarks(student.getRemarks());
        if (student.getCurrentAddress() != null) {
            Address a = student.getCurrentAddress();
            dto.setCurrentAddressLine1(a.getAddressLine1());
            dto.setCurrentAddressLine2(a.getAddressLine2());
            dto.setCurrentLandmark(a.getLandmark());
            dto.setCurrentCity(a.getCity());
            dto.setCurrentDistrict(a.getDistrict());
            dto.setCurrentState(a.getState());
            dto.setCurrentPostalCode(a.getPostalCode());
        }
        if (student.getPermanentAddress() != null) {
            Address a = student.getPermanentAddress();
            dto.setPermanentAddressLine1(a.getAddressLine1());
            dto.setPermanentAddressLine2(a.getAddressLine2());
            dto.setPermanentLandmark(a.getLandmark());
            dto.setPermanentCity(a.getCity());
            dto.setPermanentDistrict(a.getDistrict());
            dto.setPermanentState(a.getState());
            dto.setPermanentPostalCode(a.getPostalCode());
        }

        studentEnrollmentRepository.findActiveWithClassByStudentId(student.getStudentId()).ifPresent(enrollment -> {
            if (enrollment.getAcademicYear() != null) {
                dto.setAcademicYear(enrollment.getAcademicYear().getYearCode());
            }
            if (enrollment.getClassEntity() != null) {
                dto.setClassName(enrollment.getClassEntity().getClassName());
            }
            if (enrollment.getSection() != null) {
                dto.setSectionName(enrollment.getSection().getSectionName());
            }
            if (!hasText(dto.getRollNumber()) && hasText(enrollment.getRollNumber())) {
                dto.setRollNumber(enrollment.getRollNumber());
            }
        });

        resolvePrimaryParent(student.getStudentId()).ifPresent(link -> {
            dto.setParentName(buildFullName(link.getParent().getFirstName(), link.getParent().getMiddleName(), link.getParent().getLastName()));
            dto.setParentMobileNumber(link.getParent().getMobileNumber());
        });

        return dto;
    }

    private EnrollmentDTO mapEnrollment(StudentEnrollment enrollment) {
        EnrollmentDTO dto = new EnrollmentDTO();
        dto.setEnrollmentId(enrollment.getEnrollmentId());
        dto.setRollNumber(enrollment.getRollNumber());
        dto.setStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null);
        if (enrollment.getAcademicYear() != null) {
            dto.setAcademicYearId(enrollment.getAcademicYear().getAcademicYearId());
            dto.setAcademicYear(enrollment.getAcademicYear().getYearCode());
        }
        if (enrollment.getClassEntity() != null) {
            dto.setClassId(enrollment.getClassEntity().getClassId());
            dto.setClassName(enrollment.getClassEntity().getClassName());
        }
        if (enrollment.getSection() != null) {
            dto.setSectionId(enrollment.getSection().getSectionId());
            dto.setSectionName(enrollment.getSection().getSectionName());
        }
        if (enrollment.getCreatedOn() != null) {
            dto.setEnrollmentDate(enrollment.getCreatedOn().toLocalDate().toString());
        }
        return dto;
    }

    private ParentDTO mapParentLink(StudentParent sp) {
        ParentDTO dto = new ParentDTO();
        Parent parent = sp.getParent();
        dto.setParentId(parent.getParentId());
        dto.setParentCode(parent.getParentCode());
        dto.setFullName(buildFullName(parent.getFirstName(), parent.getMiddleName(), parent.getLastName()));
        dto.setMobileNumber(parent.getMobileNumber());
        dto.setEmail(parent.getEmail());
        dto.setOccupation(parent.getOccupation());
        dto.setOrganizationName(parent.getOrganizationName());
        dto.setQualification(parent.getQualification());
        dto.setRelationship(sp.getRelationship() != null ? sp.getRelationship().name() : null);
        dto.setPrimaryContact(Boolean.TRUE.equals(sp.getPrimaryContact()));
        dto.setReceiveSms(Boolean.TRUE.equals(sp.getReceiveSms()));
        dto.setReceiveEmail(Boolean.TRUE.equals(sp.getReceiveEmail()));
        dto.setPickupAuthorized(Boolean.TRUE.equals(sp.getPickupAuthorized()));
        return dto;
    }

    private String safeFileToken(String value) {
        if (value == null || value.isBlank()) {
            return String.valueOf(System.currentTimeMillis());
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String extensionOf(String fileName) {
        if (!hasText(fileName)) return null;
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : null;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "system";
        }
        return authentication.getName();
    }
}
