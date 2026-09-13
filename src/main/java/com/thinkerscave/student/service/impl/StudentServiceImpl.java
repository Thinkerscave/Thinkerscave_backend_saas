package com.thinkerscave.student.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.admission.dto.ApplicationProfileDetails;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.admission.enums.DocumentCheckStatus;
import com.thinkerscave.shared.entity.Address;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.storage.LocalFileStorageService;
import com.thinkerscave.student.dto.EnrollmentDTO;
import com.thinkerscave.student.dto.ImportedStudentDocument;
import com.thinkerscave.student.dto.MedicalDTO;
import com.thinkerscave.student.dto.ParentDTO;
import com.thinkerscave.student.dto.PreviousSchoolingDTO;
import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentDocumentDTO;
import com.thinkerscave.student.dto.StudentProfileResponse;
import com.thinkerscave.student.dto.StudentResponseDTO;
import com.thinkerscave.student.dto.StudentSearchRequest;
import com.thinkerscave.student.dto.TimelineDTO;
import com.thinkerscave.student.entity.Parent;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentDocument;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.entity.StudentMedical;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.entity.StudentTimeline;
import com.thinkerscave.student.enums.EnrollmentStatus;
import com.thinkerscave.student.enums.ParentRelationship;
import com.thinkerscave.student.enums.StudentStatus;
import com.thinkerscave.student.enums.StudentTimelineEventType;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentDocumentRepository;
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
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentMedicalRepository studentMedicalRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentTimelineRepository studentTimelineRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final AuditWriteService auditWriteService;
    private final ApplicationAdmissionRepository applicationAdmissionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StudentResponseDTO createStudent(StudentCreateRequest request) throws IOException {
        return saveStudentWithDocuments(request, null, null, null);
    }

    @Override
    @Transactional
    public StudentResponseDTO saveStudentWithDocuments(StudentCreateRequest dto, MultipartFile photo,
                                                       List<MultipartFile> documents, List<String> types) throws IOException {

        Role studentRole = roleRepository.findByRoleCode(ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("Role STUDENT not found"));
        Role parentRole = roleRepository.findByRoleCode(ROLE_PARENT)
                .orElseThrow(() -> new IllegalStateException("Role PARENT not found"));

        UserCreationContext studentContext = new UserCreationContext(
                dto.getFirstName(), dto.getMiddleName(), dto.getLastName(),
                dto.getEmail(), dto.getMobileNumber(), null, null, null, TYPE_STUDENT);
        User studentUser = userService.createUser(studentContext, studentRole);

        String studentCode = "STU" + System.currentTimeMillis();
        String admissionNumber = hasText(dto.getAdmissionNumber())
                ? dto.getAdmissionNumber().trim()
                : generateAdmissionNumber();
        if (studentRepository.existsByAdmissionNumber(admissionNumber)) {
            throw new BadRequestException("Admission number already exists: " + admissionNumber);
        }

        Parent parent = resolveOrCreateParent(dto, parentRole);

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
        student.setCategory(trimToNull(dto.getCategory()));
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

        StudentParent sp = new StudentParent();
        sp.setStudent(student);
        sp.setParent(parent);
        sp.setRelationship(parseRelationship(dto.getParentRelationship(), ParentRelationship.FATHER));
        sp.setPrimaryContact(true);
        studentParentRepository.save(sp);

        linkSecondaryParent(student, dto, parentRole);

        if (dto.getAcademicYearId() != null && dto.getClassId() != null) {
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setStudent(student);
            enrollment.setRollNumber(trimToNull(dto.getRollNumber()));
            enrollment.setStatus(parseEnrollmentStatus(dto.getEnrollmentStatus()));
            enrollment.setActive(true);
            academicYearRepository.findById(dto.getAcademicYearId()).ifPresent(enrollment::setAcademicYear);
            classRepository.findById(dto.getClassId()).ifPresent(enrollment::setClassEntity);
            if (dto.getSectionId() != null) {
                sectionRepository.findById(dto.getSectionId()).ifPresent(enrollment::setSection);
            }
            studentEnrollmentRepository.save(enrollment);
        }

        if (documents != null && types != null) {
            for (int i = 0; i < documents.size(); i++) {
                MultipartFile file = documents.get(i);
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String storedPath = fileStorageService.store(file, "doc_" + safeFileToken(studentCode));
                StudentDocument document = new StudentDocument();
                document.setStudent(student);
                document.setDocumentName(file.getOriginalFilename());
                document.setDocumentType(types.get(i) == null || types.get(i).isBlank() ? "OTHER" : types.get(i));
                document.setDocumentPath(storedPath);
                document.setStatus(DocumentCheckStatus.PENDING);
                document.setCreatedBy(getCurrentUsername());
                studentDocumentRepository.save(document);
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
        student.setCategory(trimToNull(dto.getCategory()));
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

        studentDocumentRepository.findByStudentStudentIdOrderByDocumentIdDesc(studentId).stream()
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
    public StudentResponseDTO updatePersonal(Long studentId, StudentCreateRequest dto) {
        Student student = getStudent(studentId);
        student.setFirstName(dto.getFirstName());
        student.setMiddleName(dto.getMiddleName());
        student.setLastName(dto.getLastName());
        student.setGender(dto.getGender());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setMobileNumber(dto.getMobileNumber() != null ? Long.parseLong(dto.getMobileNumber()) : null);
        student.setEmail(dto.getEmail());
        studentRepository.save(student);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Profile Updated", "Personal information was updated.");
        return mapToResponseDTO(student);
    }

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

        Page<Student> page;
        if (request != null && request.getStatus() != null
                && (request.getKeyword() == null || request.getKeyword().isBlank())
                && (request.getParentName() == null || request.getParentName().isBlank())
                && request.getClassId() == null && request.getSectionId() == null) {
            page = studentRepository.findByStatus(request.getStatus(), effectivePageable);
        } else if (request != null && request.getKeyword() != null && !request.getKeyword().isBlank()
                && request.getStatus() == null
                && (request.getParentName() == null || request.getParentName().isBlank())
                && request.getClassId() == null && request.getSectionId() == null) {
            page = studentRepository.searchByKeyword(request.getKeyword().trim(), effectivePageable);
        } else if (request == null
                || ((request.getKeyword() == null || request.getKeyword().isBlank())
                && request.getStatus() == null
                && (request.getParentName() == null || request.getParentName().isBlank())
                && request.getClassId() == null && request.getSectionId() == null)) {
            page = studentRepository.findAll(effectivePageable);
        } else {
            // Complex filters: page in DB first (cap) then refine — avoids loading entire table
            Page<Student> candidatePage = request.getStatus() != null
                    ? studentRepository.findByStatus(request.getStatus(), Pageable.ofSize(500))
                    : (request.getKeyword() != null && !request.getKeyword().isBlank()
                    ? studentRepository.searchByKeyword(request.getKeyword().trim(), Pageable.ofSize(500))
                    : studentRepository.findAll(Pageable.ofSize(500)));
            List<StudentResponseDTO> filtered = candidatePage.getContent().stream()
                    .map(this::mapToResponseDTO)
                    .filter(dto -> matchesSearch(dto, request))
                    .collect(Collectors.toList());
            int start = (int) effectivePageable.getOffset();
            int end = Math.min(start + effectivePageable.getPageSize(), filtered.size());
            List<StudentResponseDTO> content = start >= filtered.size() ? List.of() : filtered.subList(start, end);
            return new PageImpl<>(content, effectivePageable, filtered.size());
        }

        return page.map(this::mapToResponseDTO);
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
        return studentDocumentRepository.findByStudentStudentIdOrderByDocumentIdDesc(studentId)
                .stream()
                .map(this::mapDocument)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentDocumentDTO uploadStudentDocument(Long studentId, MultipartFile file, String documentType) throws IOException {
        Student student = getStudent(studentId);
        String storedPath = fileStorageService.store(file, "doc_" + safeFileToken(student.getStudentCode()));

        StudentDocument document = new StudentDocument();
        document.setStudent(student);
        document.setDocumentName(file.getOriginalFilename());
        document.setDocumentType(documentType == null || documentType.isBlank() ? "OTHER" : documentType);
        document.setDocumentPath(storedPath);
        document.setStatus(DocumentCheckStatus.PENDING);
        document.setCreatedBy(getCurrentUsername());

        StudentDocument saved = studentDocumentRepository.save(document);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Document Uploaded", "A student document was uploaded.");
        return mapDocument(saved);
    }

    @Override
    @Transactional
    public StudentDocumentDTO updateDocumentStatus(Long documentId, String status, String remarks) {
        StudentDocument document = studentDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        DocumentCheckStatus checkStatus;
        try {
            checkStatus = DocumentCheckStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid document status: " + status);
        }
        document.setStatus(checkStatus);
        if (remarks != null && !remarks.isBlank()) {
            document.setRemarks(remarks.trim());
        }
        StudentDocument saved = studentDocumentRepository.save(document);
        addTimelineEvent(document.getStudent(), StudentTimelineEventType.STUDENT_UPDATED,
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
            StudentDocument document = new StudentDocument();
            document.setStudent(student);
            document.setDocumentName(hasText(item.documentName()) ? item.documentName() : "document");
            document.setDocumentType(hasText(item.documentType()) ? item.documentType() : "OTHER");
            document.setDocumentPath(item.storedPath());
            document.setStatus(item.status() != null ? item.status() : DocumentCheckStatus.PENDING);
            document.setSourceApplicationDocumentId(item.sourceApplicationDocumentId());
            document.setRemarks(trimToNull(item.remarks()));
            document.setCreatedBy(getCurrentUsername());
            studentDocumentRepository.save(document);
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
        studentDocumentRepository.findByStudentStudentIdOrderByDocumentIdDesc(studentId).stream()
                .filter(doc -> "PHOTO".equalsIgnoreCase(doc.getDocumentType()) && hasText(doc.getDocumentPath()))
                .findFirst()
                .ifPresent(photoDoc -> {
                    student.setPhotoUrl(photoDoc.getDocumentPath());
                    studentRepository.save(student);
                });
    }

    @Override
    @Transactional
    public void deleteDocument(Long docId) {
        StudentDocument document = studentDocumentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + docId));
        studentDocumentRepository.delete(document);
    }

    @Override
    public Resource downloadDocument(Long docId) {
        StudentDocument document = studentDocumentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + docId));
        return fileStorageService.loadAsResource(document.getDocumentPath());
    }

    private void addTimelineEvent(Student student, StudentTimelineEventType type, String title, String description) {
        StudentTimeline timeline = new StudentTimeline();
        timeline.setStudent(student);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timeline.setEventType(type);
        studentTimelineRepository.save(timeline);
    }

    private boolean matchesSearch(StudentResponseDTO dto, StudentSearchRequest request) {
        if (request == null) {
            return true;
        }

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase(Locale.ROOT);
            boolean keywordMatch = containsIgnoreCase(dto.getFullName(), keyword)
                    || containsIgnoreCase(dto.getAdmissionNumber(), keyword)
                    || containsIgnoreCase(dto.getStudentCode(), keyword)
                    || containsIgnoreCase(dto.getMobileNumber(), keyword)
                    || containsIgnoreCase(dto.getEmail(), keyword);
            if (!keywordMatch) {
                return false;
            }
        }

        if (request.getStatus() != null && !Objects.equals(dto.getStatus(), request.getStatus().name())) {
            return false;
        }

        if (request.getParentName() != null && !request.getParentName().isBlank()) {
            return containsIgnoreCase(dto.getParentName(), request.getParentName().toLowerCase(Locale.ROOT));
        }

        if (request.getClassId() != null || request.getSectionId() != null) {
            Student student = studentRepository.findById(dto.getStudentId()).orElse(null);
            if (student == null) {
                return false;
            }
            StudentEnrollment enrollment = studentEnrollmentRepository.findByStudentStudentIdAndActiveTrue(student.getStudentId())
                    .orElse(null);
            if (enrollment == null) {
                return false;
            }
            if (request.getClassId() != null && (enrollment.getClassEntity() == null
                    || !Objects.equals(enrollment.getClassEntity().getClassId(), request.getClassId()))) {
                return false;
            }
            if (request.getSectionId() != null && (enrollment.getSection() == null
                    || !Objects.equals(enrollment.getSection().getSectionId(), request.getSectionId()))) {
                return false;
            }
        }

        return true;
    }

    private boolean containsIgnoreCase(String source, String valueLower) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(valueLower);
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

    private Parent resolveOrCreateParent(StudentCreateRequest dto, Role parentRole) {
        if (dto.getExistingParentId() != null) {
            return parentRepository.findById(dto.getExistingParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent not found: " + dto.getExistingParentId()));
        }
        String mobile = dto.getParentMobileNumber() != null ? dto.getParentMobileNumber().trim() : null;
        if (mobile != null && !mobile.isBlank()) {
            java.util.Optional<Parent> existing = parentRepository.findByMobileNumber(mobile);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        UserCreationContext guardianContext = new UserCreationContext(
                dto.getParentFirstName(), dto.getParentMiddleName(), dto.getParentLastName(),
                dto.getParentEmail(), dto.getParentMobileNumber(), null, null, null, TYPE_GUARDIAN);
        User parentUser = userService.createUser(guardianContext, parentRole);

        Parent parent = new Parent();
        parent.setParentCode("PAR" + System.currentTimeMillis());
        parent.setFirstName(dto.getParentFirstName());
        parent.setMiddleName(dto.getParentMiddleName());
        parent.setLastName(hasText(dto.getParentLastName()) ? dto.getParentLastName().trim() : "NA");
        parent.setMobileNumber(dto.getParentMobileNumber());
        parent.setEmail(dto.getParentEmail());
        parent.setOccupation(dto.getParentOccupation());
        parent.setOrganizationName(dto.getParentOrganizationName());
        parent.setQualification(dto.getParentQualification());
        parent.setAnnualIncome(dto.getAnnualIncome());
        parent.setUser(parentUser);
        return parentRepository.save(parent);
    }

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

    private StudentDocumentDTO mapDocument(StudentDocument document) {
        StudentDocumentDTO dto = new StudentDocumentDTO();
        dto.setDocumentId(document.getDocumentId());
        dto.setDocumentName(document.getDocumentName());
        dto.setDocumentType(document.getDocumentType());
        dto.setStatus(document.getStatus() != null ? document.getStatus() : DocumentCheckStatus.PENDING);
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
                dto.getCurrentCity(),
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
                dto.getPermanentCity(),
                dto.getPermanentState(),
                dto.getPermanentCountry(),
                dto.getPermanentPostalCode());
        if (permanent != null) {
            student.setPermanentAddress(permanent);
        }
    }

    private Address buildAddress(String line1, String line2, String city, String state, String country, String postal) {
        if (!hasText(line1) && !hasText(city) && !hasText(state) && !hasText(postal)) {
            return null;
        }
        Address address = new Address();
        address.setAddressLine1(hasText(line1) ? line1.trim() : "—");
        address.setAddressLine2(trimToNull(line2));
        address.setCity(trimToNull(city));
        address.setState(trimToNull(state));
        address.setCountry(hasText(country) ? country.trim() : "India");
        address.setPostalCode(trimToNull(postal));
        address.setActive(true);
        return address;
    }

    private Address copyAddress(Address source) {
        Address copy = new Address();
        copy.setAddressLine1(source.getAddressLine1());
        copy.setAddressLine2(source.getAddressLine2());
        copy.setCity(source.getCity());
        copy.setState(source.getState());
        copy.setCountry(source.getCountry());
        copy.setPostalCode(source.getPostalCode());
        copy.setLandmark(source.getLandmark());
        copy.setDistrict(source.getDistrict());
        copy.setActive(true);
        return copy;
    }

    private void linkSecondaryParent(Student student, StudentCreateRequest dto, Role parentRole) {
        if (!hasText(dto.getSecondaryParentFirstName()) || !hasText(dto.getSecondaryParentMobileNumber())) {
            return;
        }
        StudentCreateRequest secondary = new StudentCreateRequest();
        secondary.setParentFirstName(dto.getSecondaryParentFirstName());
        secondary.setParentLastName(hasText(dto.getSecondaryParentLastName()) ? dto.getSecondaryParentLastName() : "NA");
        secondary.setParentMobileNumber(dto.getSecondaryParentMobileNumber());
        secondary.setParentEmail(dto.getSecondaryParentEmail());
        secondary.setParentOccupation(dto.getSecondaryParentOccupation());
        Parent parent = resolveOrCreateParent(secondary, parentRole);
        StudentParent link = new StudentParent();
        link.setStudent(student);
        link.setParent(parent);
        link.setRelationship(parseRelationship(dto.getSecondaryParentRelationship(), ParentRelationship.MOTHER));
        link.setPrimaryContact(false);
        studentParentRepository.save(link);
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

    private String generateAdmissionNumber() {
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        String candidate;
        do {
            candidate = "ADM-" + year + "-" + (10000 + (System.nanoTime() % 89999));
        } while (studentRepository.existsByAdmissionNumber(candidate));
        return candidate;
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
        dto.setCategory(student.getCategory());
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
            dto.setCurrentCity(a.getCity());
            dto.setCurrentState(a.getState());
            dto.setCurrentPostalCode(a.getPostalCode());
        }
        if (student.getPermanentAddress() != null) {
            Address a = student.getPermanentAddress();
            dto.setPermanentAddressLine1(a.getAddressLine1());
            dto.setPermanentAddressLine2(a.getAddressLine2());
            dto.setPermanentCity(a.getCity());
            dto.setPermanentState(a.getState());
            dto.setPermanentPostalCode(a.getPostalCode());
        }

        studentEnrollmentRepository.findActiveWithClassByStudentId(student.getStudentId()).ifPresent(enrollment -> {
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
            dto.setAcademicYear(enrollment.getAcademicYear().getYearCode());
        }
        if (enrollment.getClassEntity() != null) {
            dto.setClassName(enrollment.getClassEntity().getClassName());
        }
        if (enrollment.getSection() != null) {
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
        dto.setRelationship(sp.getRelationship() != null ? sp.getRelationship().name() : null);
        dto.setPrimaryContact(Boolean.TRUE.equals(sp.getPrimaryContact()));
        return dto;
    }

    private String safeFileToken(String value) {
        if (value == null || value.isBlank()) {
            return String.valueOf(System.currentTimeMillis());
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "system";
        }
        return authentication.getName();
    }
}
