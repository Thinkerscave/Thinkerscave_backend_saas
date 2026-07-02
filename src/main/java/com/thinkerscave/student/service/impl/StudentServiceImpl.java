package com.thinkerscave.student.service.impl;

import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.dto.EnrollmentDTO;
import com.thinkerscave.student.dto.MedicalDTO;
import com.thinkerscave.student.dto.ParentDTO;
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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_PARENT = "PARENT";
    private static final String TYPE_STUDENT = "student";
    private static final String TYPE_GUARDIAN = "guardian";

    private final Path rootLocation = Paths.get("uploads");

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

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

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

        UserCreationContext guardianContext = new UserCreationContext(
                dto.getParentFirstName(), dto.getParentMiddleName(), dto.getParentLastName(),
                dto.getParentEmail(), dto.getParentMobileNumber(), null, null, null, TYPE_GUARDIAN);
        User parentUser = userService.createUser(guardianContext, parentRole);

        String parentCode = "PAR" + System.currentTimeMillis();
        String studentCode = "STU" + System.currentTimeMillis();

        Parent parent = new Parent();
        parent.setParentCode(parentCode);
        parent.setFirstName(dto.getParentFirstName());
        parent.setMiddleName(dto.getParentMiddleName());
        parent.setLastName(dto.getParentLastName());
        parent.setMobileNumber(dto.getParentMobileNumber());
        parent.setEmail(dto.getParentEmail());
        parent.setOccupation(dto.getParentOccupation());
        parent.setOrganizationName(dto.getParentOrganizationName());
        parent.setQualification(dto.getParentQualification());
        parent.setAnnualIncome(dto.getAnnualIncome());
        parent.setUser(parentUser);
        parent = parentRepository.save(parent);

        Student student = new Student();
        student.setStudentCode(studentCode);
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
        student.setMobileNumber(dto.getMobileNumber() != null ? Long.parseLong(dto.getMobileNumber()) : null);
        student.setEmail(dto.getEmail());
        student.setRemarks(dto.getRemarks());
        student.setStatus(StudentStatus.ACTIVE);
        student.setUser(studentUser);

        if (photo != null && !photo.isEmpty()) {
            student.setPhotoUrl(saveFile(photo, "photo_" + safeFileToken(studentCode)));
        }

        student = studentRepository.save(student);

        StudentMedical medical = new StudentMedical();
        medical.setStudent(student);
        medical.setBloodGroup(dto.getBloodGroup());
        medical.setAllergies(dto.getAllergies());
        medical.setMedicalConditions(dto.getMedicalConditions());
        medical.setMedications(dto.getMedications());
        medical.setDoctorName(dto.getDoctorName());
        medical.setDoctorContact(dto.getDoctorContact());
        medical.setEmergencyNotes(dto.getEmergencyNotes());
        studentMedicalRepository.save(medical);

        StudentParent sp = new StudentParent();
        sp.setStudent(student);
        sp.setParent(parent);
        sp.setRelationship(ParentRelationship.FATHER);
        sp.setPrimaryContact(true);
        studentParentRepository.save(sp);

        if (dto.getAcademicYearId() != null && dto.getClassId() != null) {
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setStudent(student);
            enrollment.setRollNumber(dto.getRollNumber());
            enrollment.setStatus(EnrollmentStatus.ACTIVE);

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
                String storedPath = saveFile(file, "doc_" + safeFileToken(studentCode));
                StudentDocument document = new StudentDocument();
                document.setStudent(student);
                document.setDocumentName(file.getOriginalFilename());
                document.setDocumentType(types.get(i));
                document.setDocumentPath(storedPath);
                document.setCreatedBy(getCurrentUsername());
                studentDocumentRepository.save(document);
            }
        }

        addTimelineEvent(student, StudentTimelineEventType.STUDENT_CREATED,
                "Student Admitted", "Student successfully registered in system.");

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
        student.setMobileNumber(dto.getMobileNumber() != null ? Long.parseLong(dto.getMobileNumber()) : null);
        student.setEmail(dto.getEmail());
        student.setRemarks(dto.getRemarks());
        studentRepository.save(student);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Student Updated", "Student profile details were updated.");
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

        studentEnrollmentRepository.findByStudentStudentIdAndActiveTrue(studentId).ifPresent(enrollment -> {
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
            response.setEnrollment(dto);
        });

        resolvePrimaryParent(studentId).ifPresent(sp -> {
            ParentDTO dto = new ParentDTO();
            dto.setParentId(sp.getParent().getParentId());
            dto.setParentCode(sp.getParent().getParentCode());
            dto.setFullName(buildFullName(sp.getParent().getFirstName(), sp.getParent().getMiddleName(), sp.getParent().getLastName()));
            dto.setMobileNumber(sp.getParent().getMobileNumber());
            dto.setEmail(sp.getParent().getEmail());
            dto.setOccupation(sp.getParent().getOccupation());
            response.setParent(dto);
        });

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
        List<StudentResponseDTO> filtered = studentRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .filter(dto -> matchesSearch(dto, request))
                .collect(Collectors.toList());

        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(filtered);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<StudentResponseDTO> content = start >= filtered.size() ? List.of() : filtered.subList(start, end);
        return new PageImpl<>(content, pageable, filtered.size());
    }

    @Override
    public List<StudentResponseDTO> getAllStudents() {
        return studentRepository.findAll().stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = getStudent(studentId);
        student.setStatus(StudentStatus.INACTIVE);
        studentRepository.save(student);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Student Deactivated", "Student status changed to inactive.");
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
        String storedPath = saveFile(file, "doc_" + safeFileToken(student.getStudentCode()));

        StudentDocument document = new StudentDocument();
        document.setStudent(student);
        document.setDocumentName(file.getOriginalFilename());
        document.setDocumentType(documentType == null || documentType.isBlank() ? "OTHER" : documentType);
        document.setDocumentPath(storedPath);
        document.setCreatedBy(getCurrentUsername());

        StudentDocument saved = studentDocumentRepository.save(document);
        addTimelineEvent(student, StudentTimelineEventType.STUDENT_UPDATED,
                "Document Uploaded", "A student document was uploaded.");
        return mapDocument(saved);
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
        Path path = Paths.get(document.getDocumentPath()).normalize();
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Document file not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid document path", e);
        }
    }

    private void addTimelineEvent(Student student, StudentTimelineEventType type, String title, String description) {
        StudentTimeline timeline = new StudentTimeline();
        timeline.setStudent(student);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timeline.setEventType(type);
        studentTimelineRepository.save(timeline);
    }

    private String saveFile(MultipartFile file, String prefix) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Path destination = rootLocation.resolve(prefix + "_" + safeFileToken(file.getOriginalFilename()))
                .normalize().toAbsolutePath();
        if (!destination.getParent().equals(rootLocation.toAbsolutePath())) {
            throw new IOException("Cannot store file outside current directory.");
        }
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination.toString();
        }
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
        return dto;
    }

    private StudentResponseDTO mapToResponseDTO(Student student) {
        StudentResponseDTO dto = new StudentResponseDTO();
        dto.setStudentId(student.getStudentId());
        dto.setStudentCode(student.getStudentCode());
        dto.setAdmissionNumber(student.getAdmissionNumber());
        dto.setFullName(buildFullName(student.getFirstName(), student.getMiddleName(), student.getLastName()));
        dto.setMobileNumber(student.getMobileNumber() != null ? student.getMobileNumber().toString() : "");
        dto.setEmail(student.getEmail());
        dto.setStatus(student.getStatus() != null ? student.getStatus().name() : StudentStatus.ACTIVE.name());

        studentEnrollmentRepository.findByStudentStudentIdAndActiveTrue(student.getStudentId()).ifPresent(enrollment -> {
            if (enrollment.getClassEntity() != null) {
                dto.setClassName(enrollment.getClassEntity().getClassName());
            }
            if (enrollment.getSection() != null) {
                dto.setSectionName(enrollment.getSection().getSectionName());
            }
        });

        resolvePrimaryParent(student.getStudentId()).ifPresent(link -> {
            dto.setParentName(buildFullName(link.getParent().getFirstName(), link.getParent().getMiddleName(), link.getParent().getLastName()));
            dto.setParentMobileNumber(link.getParent().getMobileNumber());
        });

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
