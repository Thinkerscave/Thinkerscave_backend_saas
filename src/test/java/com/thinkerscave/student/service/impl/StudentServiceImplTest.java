package com.thinkerscave.student.service.impl;

import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.dto.UserProvisioningResult;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.repository.UserRoleRepository;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.security.service.UserCredentialNotificationService;
import com.thinkerscave.shared.storage.LocalFileStorageService;
import com.thinkerscave.shared.service.AddressFactory;
import com.thinkerscave.shared.entity.Address;
import com.thinkerscave.document.entity.Document;
import com.thinkerscave.document.enums.DocumentOwnerType;
import com.thinkerscave.document.repository.DocumentRepository;
import com.thinkerscave.student.entity.Parent;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentMedical;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentMedicalRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.repository.StudentTimelineRepository;
import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentPersonalUpdateRequest;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.admission.enums.DocumentCheckStatus;
import com.thinkerscave.student.dto.StudentParentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ByteArrayResource;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock private LocalFileStorageService fileStorageService;
    @Mock private AddressFactory addressFactory;
    @Mock private UserService userService;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private com.thinkerscave.academics.repository.ClassRepository classRepository;
    @Mock private com.thinkerscave.academics.repository.SectionRepository sectionRepository;
    @Mock private com.thinkerscave.academics.repository.AcademicYearRepository academicYearRepository;
    @Mock private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock private StudentMedicalRepository studentMedicalRepository;
    @Mock private StudentParentRepository studentParentRepository;
    @Mock private StudentTimelineRepository studentTimelineRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private AuditWriteService auditWriteService;
    @Mock private ApplicationAdmissionRepository applicationAdmissionRepository;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock private UserCredentialNotificationService credentialNotificationService;

    @InjectMocks private StudentServiceImpl service;

    @Test
    void directCreationLeavesAdmissionNumberBlankWhenNotProvided() throws Exception {
        when(parentRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setStudentId(41L);
            return student;
        });
        when(studentEnrollmentRepository.findActiveWithClassByStudentId(41L)).thenReturn(Optional.empty());
        when(studentParentRepository.findByStudentStudentId(41L)).thenReturn(List.<StudentParent>of());

        StudentCreateRequest request = new StudentCreateRequest();
        request.setFirstName("Asha");
        request.setLastName("Rao");
        request.setGender("FEMALE");
        request.setDateOfBirth(LocalDate.of(2014, 4, 10));
        request.setParentFirstName("Mira");
        request.setParentLastName("Rao");
        request.setParentMobileNumber("9876543210");

        service.createStudent(request);

        ArgumentCaptor<Student> savedStudent = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(savedStudent.capture());
        assertNull(savedStudent.getValue().getAdmissionNumber());
        assertNull(savedStudent.getValue().getUser());
        verify(studentRepository, never()).existsByAdmissionNumber(any());
        verify(userService, never()).createUserWithTemporaryPassword(any(), any());
    }

    @Test
    void directCreationPreservesManuallyEnteredAdmissionNumber() throws Exception {
        Role studentRole = new Role();
        User studentUser = new User();
        UserProvisioningResult provisioning = new UserProvisioningResult(studentUser, "temporary");

        when(roleRepository.findByRoleCode("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmailIgnoreCase("ravi.das@example.test")).thenReturn(Optional.empty());
        when(userService.createUserWithTemporaryPassword(any(UserCreationContext.class), eq(studentRole)))
                .thenReturn(provisioning);
        when(parentRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.existsByAdmissionNumber("MANUAL-42")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setStudentId(42L);
            return student;
        });
        when(studentEnrollmentRepository.findActiveWithClassByStudentId(42L)).thenReturn(Optional.empty());
        when(studentParentRepository.findByStudentStudentId(42L)).thenReturn(List.<StudentParent>of());

        StudentCreateRequest request = new StudentCreateRequest();
        request.setAdmissionNumber(" MANUAL-42 ");
        request.setFirstName("Ravi");
        request.setLastName("Das");
        request.setGender("MALE");
        request.setDateOfBirth(LocalDate.of(2013, 5, 2));
        request.setEmail("ravi.das@example.test");
        request.setParentFirstName("Sita");
        request.setParentLastName("Das");
        request.setParentMobileNumber("9876543210");

        service.createStudent(request);

        ArgumentCaptor<Student> savedStudent = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(savedStudent.capture());
        assertEquals("MANUAL-42", savedStudent.getValue().getAdmissionNumber());
        verify(studentRepository).existsByAdmissionNumber("MANUAL-42");
    }

    @Test
    void directCreationPersistsAllGuardiansWithExactlyOnePrimary() throws Exception {
        when(parentRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(parentRepository.findByMobileNumber("9876543211")).thenReturn(Optional.empty());
        when(parentRepository.findByMobileNumber("9876543212")).thenReturn(Optional.empty());
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setStudentId(43L);
            return student;
        });
        when(studentEnrollmentRepository.findActiveWithClassByStudentId(43L)).thenReturn(Optional.empty());
        when(studentParentRepository.findByStudentStudentId(43L)).thenReturn(List.<StudentParent>of());

        StudentCreateRequest request = new StudentCreateRequest();
        request.setFirstName("Maya");
        request.setLastName("Patel");
        request.setGender("FEMALE");
        request.setDateOfBirth(LocalDate.of(2015, 6, 12));
        request.setParents(List.of(
                parent("Mother", "Mira", "9876543210", true),
                parent("Father", "Raj", "9876543211", false),
                parent("Guardian", "Neha", "9876543212", false)));

        service.createStudent(request);

        ArgumentCaptor<StudentParent> links = ArgumentCaptor.forClass(StudentParent.class);
        verify(studentParentRepository, org.mockito.Mockito.times(3)).save(links.capture());
        assertEquals(1, links.getAllValues().stream().filter(link -> Boolean.TRUE.equals(link.getPrimaryContact())).count());
        assertTrue(links.getAllValues().stream().anyMatch(link -> link.getRelationship().name().equals("GUARDIAN")));
    }

    @Test
    void personalUpdatePersistsContactAndAddressWithoutChangingPlacementIdentity() {
        Student student = new Student();
        student.setStudentId(44L);
        student.setStudentCode("STU-44");
        student.setAdmissionNumber("ADM-44");
        student.setRollNumber("R-10");
        student.setFirstName("Maya");
        student.setLastName("Rao");
        Address address = new Address();
        address.setAddressLine1("Old Road");
        address.setLandmark("Old Landmark");
        address.setCity("Pune");
        address.setDistrict("Pune District");
        address.setState("Maharashtra");
        student.setCurrentAddress(address);
        Address permanentAddress = new Address();
        permanentAddress.setAddressLine1("Old Permanent Road");
        permanentAddress.setCity("Pune");
        student.setPermanentAddress(permanentAddress);
        when(studentRepository.findById(44L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentEnrollmentRepository.findActiveWithClassByStudentId(44L)).thenReturn(Optional.empty());
        when(studentParentRepository.findByStudentStudentId(44L)).thenReturn(List.of());

        StudentPersonalUpdateRequest request = new StudentPersonalUpdateRequest();
        request.setFirstName("Maya");
        request.setLastName("Patel");
        request.setMobile("9876543210");
        request.setEmail("");
        request.setCurrentAddressLine1("New Road");
        request.setCurrentLandmark("Near Station");
        request.setCurrentCity("Mumbai");
        request.setCurrentDistrict("Mumbai Suburban");
        request.setCurrentState("Maharashtra");
        request.setSameAddress(false);
        request.setPermanentAddressLine1("New Permanent Road");
        request.setPermanentCity("Nashik");
        request.setPermanentDistrict("Nashik District");

        service.updatePersonal(44L, request);

        assertEquals("Patel", student.getLastName());
        assertEquals(9876543210L, student.getMobileNumber());
        assertNull(student.getEmail());
        assertEquals("New Road", student.getCurrentAddress().getAddressLine1());
        assertEquals("Near Station", student.getCurrentAddress().getLandmark());
        assertEquals("Mumbai", student.getCurrentAddress().getCity());
        assertEquals("Mumbai Suburban", student.getCurrentAddress().getDistrict());
        assertEquals("ADM-44", student.getAdmissionNumber());
        assertEquals("R-10", student.getRollNumber());
        assertEquals("New Permanent Road", student.getPermanentAddress().getAddressLine1());
        assertEquals("Nashik", student.getPermanentAddress().getCity());
        assertEquals("Nashik District", student.getPermanentAddress().getDistrict());
        verify(studentEnrollmentRepository, never()).save(any());
    }

    @Test
    void directCreationRejectsUnknownAcademicYearBeforeCreatingStudent() {
        when(academicYearRepository.findById(99L)).thenReturn(Optional.empty());
        StudentCreateRequest request = new StudentCreateRequest();
        request.setFirstName("Asha");
        request.setLastName("Rao");
        request.setAcademicYearId(99L);
        request.setClassId(8L);
        request.setParentFirstName("Mira");
        request.setParentMobileNumber("9876543210");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createStudent(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(studentRepository, never()).save(any(Student.class));
        verify(studentEnrollmentRepository, never()).save(any());
        verify(userService, never()).createUserWithTemporaryPassword(any(), any());
    }

    @Test
    void genericStudentStatusUpdateCannotBypassTransferLifecycle() {
        Student transferred = new Student();
        transferred.setStudentId(45L);
        transferred.setStatus(com.thinkerscave.student.enums.StudentStatus.TRANSFERRED);
        when(studentRepository.findById(45L)).thenReturn(Optional.of(transferred));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.updateStudentStatus(45L, com.thinkerscave.student.enums.StudentStatus.ACTIVE))
                .isInstanceOf(com.thinkerscave.shared.exceptions.BadRequestException.class);

        verify(studentRepository, never()).save(any(Student.class));
    }

        @Test
        void sharedStudentDocumentLifecyclePreservesReplacedVersionsAndSoftRemoval() throws Exception {
        Student student = new Student();
        student.setStudentId(44L);
        student.setStudentCode("STU-44");
        when(studentRepository.findById(44L)).thenReturn(Optional.of(student));
        when(fileStorageService.store(any(org.springframework.web.multipart.MultipartFile.class), any()))
            .thenReturn("students/44/id-old.pdf", "students/44/id-new.pdf");
        when(fileStorageService.sanitizeFilename("id.pdf")).thenReturn("id.pdf");
        when(fileStorageService.loadAsResource("students/44/id-new.pdf"))
            .thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        Map<Long, Document> stored = new ConcurrentHashMap<>();
        AtomicLong ids = new AtomicLong(50L);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            if (document.getDocumentId() == null) document.setDocumentId(ids.incrementAndGet());
            stored.put(document.getDocumentId(), document);
            return document;
        });
        when(documentRepository.findById(any(Long.class))).thenAnswer(invocation ->
            Optional.ofNullable(stored.get(invocation.getArgument(0))));

        MockMultipartFile firstFile = new MockMultipartFile("file", "id.pdf", "application/pdf", new byte[]{1});
        var first = service.uploadStudentDocument(44L, firstFile, "AADHAAR", "Identity proof", "Initial upload");
        assertEquals(51L, first.getDocumentId());
        assertEquals(DocumentOwnerType.STUDENT, stored.get(51L).getOwnerType());

        MockMultipartFile replacementFile = new MockMultipartFile("file", "id.pdf", "application/pdf", new byte[]{2});
        var replacement = service.replaceStudentDocument(44L, 51L, replacementFile, "Updated identity", null);
        assertEquals(52L, replacement.getDocumentId());
        assertEquals(51L, replacement.getReplacedDocumentId());
        assertNull(stored.get(52L).getSourceDocumentType());
        assertEquals(false, stored.get(51L).getActive());

        var verified = service.updateDocumentStatus(52L, "VERIFIED", "Checked");
        assertEquals(DocumentCheckStatus.VERIFIED, verified.getStatus());
        assertEquals(3, service.downloadDocument(52L).getContentAsByteArray().length);

        service.deleteDocument(52L);
        assertEquals(false, stored.get(52L).getActive());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.downloadDocument(52L))
            .isInstanceOf(com.thinkerscave.shared.exceptions.ResourceNotFoundException.class);
        }

    private static StudentParentRequest parent(String relationship, String firstName, String mobile, boolean primary) {
        StudentParentRequest request = new StudentParentRequest();
        request.setRelationship(relationship);
        request.setFirstName(firstName);
        request.setLastName("Family");
        request.setMobileNumber(mobile);
        request.setPrimaryContact(primary);
        return request;
    }
}