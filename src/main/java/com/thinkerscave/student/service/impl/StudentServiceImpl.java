package com.thinkerscave.student.service.impl;

import org.springframework.transaction.annotation.Transactional;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.enums.EnrollmentStatus;
import com.thinkerscave.student.enums.ParentRelationship;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.student.entity.Parent;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentMedical;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.entity.StudentTimeline;
import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentResponseDTO;
import com.thinkerscave.student.dto.StudentProfileResponse;
import com.thinkerscave.student.dto.EnrollmentDTO;
import com.thinkerscave.student.dto.ParentDTO;
import com.thinkerscave.student.dto.MedicalDTO;
import com.thinkerscave.student.dto.TimelineDTO;
import com.thinkerscave.student.dto.StudentDocumentDTO;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.repository.StudentMedicalRepository;
import com.thinkerscave.student.repository.StudentTimelineRepository;
import com.thinkerscave.student.service.StudentService;
import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.access.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ArrayList;
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
        
        // 1. Resolve Role
        Role studentRole = roleRepository.findByRoleCode(ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("Role STUDENT not found"));
        Role parentRole = roleRepository.findByRoleCode(ROLE_PARENT)
                .orElseThrow(() -> new IllegalStateException("Role PARENT not found"));

        // 2. Invoke UserService for both
        UserCreationContext studentContext = new UserCreationContext(
                dto.getFirstName(), dto.getMiddleName(), dto.getLastName(),
                dto.getEmail(), dto.getMobileNumber(), null, null, null, TYPE_STUDENT);
        User studentUser = userService.createUser(studentContext, studentRole);

        UserCreationContext guardianContext = new UserCreationContext(
                dto.getParentFirstName(), dto.getParentMiddleName(), dto.getParentLastName(),
                dto.getParentEmail(), dto.getParentMobileNumber(), null, null, null, TYPE_GUARDIAN);
        User parentUser = userService.createUser(guardianContext, parentRole);

        // 3. Generate Codes
        String parentCode = "PAR" + System.currentTimeMillis();
        String studentCode = "STU" + System.currentTimeMillis();

        // 4. Save Parent
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

        // 5. Save Student
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
        student.setUser(studentUser);
        
        if (photo != null && !photo.isEmpty()) {
            student.setPhotoUrl(saveFile(photo, "photo_" + dto.getEmail()));
        }
        
        student = studentRepository.save(student);

        // 6. Save StudentMedical
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

        // 7. Save StudentParent relation
        StudentParent sp = new StudentParent();
        sp.setStudent(student);
        sp.setParent(parent);
        sp.setRelationship(ParentRelationship.FATHER);
        sp.setPrimaryContact(true);
        studentParentRepository.save(sp);

        // 8. Save Enrollment
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

        // 9. Timeline Event
        addTimelineEvent(student, "STUDENT_CREATED", "Student Admitted", "Student successfully registered in system.");

        return mapToResponseDTO(student);
    }

    private void addTimelineEvent(Student student, String type, String title, String description) {
        StudentTimeline timeline = new StudentTimeline();
        timeline.setStudent(student);
        timeline.setTitle(title);
        timeline.setDescription(description);
        try {
            timeline.setEventType(com.thinkerscave.student.enums.StudentTimelineEventType.valueOf(type));
        } catch (IllegalArgumentException e) {
            timeline.setEventType(com.thinkerscave.student.enums.StudentTimelineEventType.STUDENT_UPDATED);
        }
        studentTimelineRepository.save(timeline);
    }

    private String saveFile(MultipartFile file, String prefix) throws IOException {
        if (file == null || file.isEmpty()) return null;
        Path destination = rootLocation.resolve(prefix + "_" + file.getOriginalFilename()).normalize().toAbsolutePath();
        if (!destination.getParent().equals(rootLocation.toAbsolutePath())) {
            throw new IOException("Cannot store file outside current directory.");
        }
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination.toString();
        }
    }

    @Override
    @Transactional
    public StudentResponseDTO updateStudent(Long studentId, StudentCreateRequest dto) {
        return updatePersonal(studentId, dto);
    }

    @Override
    public StudentResponseDTO getStudentById(Long studentId) {
        Long orgId = com.thinkerscave.shared.context.OrganizationContext.getOrganizationId();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return mapToResponseDTO(student);
    }

    @Override
    public StudentProfileResponse getProfile360(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        StudentProfileResponse response = new StudentProfileResponse();
        response.setStudent(mapToResponseDTO(student));

        // Enrollment
        studentEnrollmentRepository.findByStudentStudentIdAndActiveTrue(studentId).ifPresent(enrollment -> {
            EnrollmentDTO dto = new EnrollmentDTO();
            dto.setEnrollmentId(enrollment.getEnrollmentId());
            dto.setRollNumber(enrollment.getRollNumber());
            dto.setStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null);
            if (enrollment.getAcademicYear() != null) dto.setAcademicYear(enrollment.getAcademicYear().getYearCode());
            if (enrollment.getClassEntity() != null) dto.setClassName(enrollment.getClassEntity().getClassName());
            if (enrollment.getSection() != null) dto.setSectionName(enrollment.getSection().getSectionName());
            response.setEnrollment(dto);
        });

        // Parent
        studentParentRepository.findByStudentStudentId(studentId).stream()
                .filter(sp -> Boolean.TRUE.equals(sp.getPrimaryContact()))
                .findFirst()
                .ifPresent(sp -> {
                    ParentDTO dto = new ParentDTO();
                    dto.setParentId(sp.getParent().getParentId());
                    dto.setParentCode(sp.getParent().getParentCode());
                    dto.setFullName(sp.getParent().getFirstName() + " " + sp.getParent().getLastName());
                    dto.setMobileNumber(sp.getParent().getMobileNumber());
                    dto.setEmail(sp.getParent().getEmail());
                    dto.setOccupation(sp.getParent().getOccupation());
                    response.setParent(dto);
                });

        // Medical
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

        // Timeline
        response.setTimeline(studentTimelineRepository.findByStudentStudentIdOrderByCreatedOnDesc(studentId)
                .stream()
                .map(t -> {
                    TimelineDTO dto = new TimelineDTO();
                    dto.setTimelineId(t.getTimelineId());
                    dto.setTitle(t.getTitle());
                    dto.setDescription(t.getDescription());
                    dto.setEventType(t.getEventType() != null ? t.getEventType().name() : null);
                    dto.setCreatedDate(t.getCreatedOn() != null
                            ? t.getCreatedOn().toInstant(java.time.ZoneOffset.UTC) : null);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList()));

        return response;
    }

    @Override
    @Transactional
    public StudentResponseDTO updatePersonal(Long studentId, StudentCreateRequest dto) {
        Student student = studentRepository.findById(studentId).orElseThrow();
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        studentRepository.save(student);
        addTimelineEvent(student, "UPDATE", "Profile Updated", "Personal information was updated.");
        return mapToResponseDTO(student);
    }

    @Override
    @Transactional
    public StudentResponseDTO updateMedical(Long studentId, MedicalDTO dto) {
        Student student = studentRepository.findById(studentId).orElseThrow();
        StudentMedical medical = studentMedicalRepository.findByStudentStudentId(studentId)
                .orElse(new StudentMedical());
        medical.setStudent(student);
        medical.setBloodGroup(dto.getBloodGroup());
        medical.setAllergies(dto.getAllergies());
        studentMedicalRepository.save(medical);
        return mapToResponseDTO(student);
    }

    @Override
    public List<TimelineDTO> getTimeline(Long studentId) {
        return new ArrayList<>();
    }

    @Override
    public Page<StudentResponseDTO> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable).map(this::mapToResponseDTO);
    }

    @Override
    public List<StudentResponseDTO> getAllStudents() {
        return studentRepository.findAll().stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId).orElseThrow();
        student.setStatus(com.thinkerscave.student.enums.StudentStatus.INACTIVE);
        studentRepository.save(student);
    }

    @Override
    public List<StudentDocumentDTO> getStudentDocuments(Long studentId) {
        return new ArrayList<>();
    }

    @Override
    public Resource downloadDocument(Long docId) {
        return null;
    }

    private StudentResponseDTO mapToResponseDTO(Student student) {
        StudentResponseDTO dto = new StudentResponseDTO();
        dto.setStudentId(student.getStudentId());
        dto.setStudentCode(student.getStudentCode());
        dto.setAdmissionNumber(student.getAdmissionNumber());
        dto.setFullName(student.getFirstName() + " " + student.getLastName());
        dto.setMobileNumber(student.getMobileNumber() != null ? student.getMobileNumber().toString() : "");
        dto.setEmail(student.getEmail());
        dto.setStatus(student.getStatus().name());
        return dto;
    }
}
