package com.thinkerscave.admission.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.academics.dto.response.LookupDTO;
import com.thinkerscave.academics.service.AcademicsLookupService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.admission.dto.ApplicationProfileDetails;
import com.thinkerscave.admission.dto.request.ApplicationAdmissionRequest;
import com.thinkerscave.admission.dto.request.ApplicationSearchRequest;
import com.thinkerscave.admission.dto.request.EnrollApplicationRequest;
import com.thinkerscave.admission.dto.request.RecordFeeRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.dto.response.ApplicationDocumentFile;
import com.thinkerscave.admission.dto.response.ApplicationDocumentResponse;
import com.thinkerscave.admission.dto.response.ApplicationProgressResponse;
import com.thinkerscave.admission.dto.response.EnrollmentResultResponse;
import com.thinkerscave.admission.dto.response.FamilyMatchResponse;
import com.thinkerscave.admission.entity.AdmissionApplicationDocument;
import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.DocumentCheckStatus;
import com.thinkerscave.admission.enums.FeePaymentStatus;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.repository.AdmissionApplicationDocumentRepository;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.admission.service.AdmissionsSettingService;
import com.thinkerscave.admission.service.ApplicationAdmissionService;
import com.thinkerscave.admission.specification.ApplicationAdmissionSpecification;
import com.thinkerscave.admission.util.RequiredDocumentsResolver;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.storage.LocalFileStorageService;
import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentResponseDTO;
import com.thinkerscave.student.entity.Parent;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApplicationAdmissionServiceImpl implements ApplicationAdmissionService {

    private static final Set<ApplicationStatus> EDITABLE = EnumSet.of(
            ApplicationStatus.DRAFT,
            ApplicationStatus.ACTION_REQUIRED,
            ApplicationStatus.DOCUMENTS_PENDING
    );
    private static final Set<ApplicationStatus> SUBMITTABLE = EnumSet.of(
            ApplicationStatus.DRAFT,
            ApplicationStatus.ACTION_REQUIRED,
            ApplicationStatus.DOCUMENTS_PENDING
    );
    private static final Set<ApplicationStatus> REVIEWABLE = EnumSet.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.DOCUMENTS_PENDING,
            ApplicationStatus.FEE_PENDING
    );
    private static final String RESOURCE_ADMISSIONS_APPLICATIONS = "ADMISSIONS_APPLICATIONS";

    private final ApplicationAdmissionRepository repository;
    private final AdmissionApplicationDocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    private final InquiryRepository inquiryRepository;
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final AdmissionsSettingService settingService;
    private final AcademicsLookupService academicsLookupService;
    private final LocalFileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final AuditWriteService auditWriteService;

    @Override
    @Transactional
    public ApplicationAdmissionResponse saveDraft(ApplicationAdmissionRequest request) {
        requireManageApplications();
        ApplicationAdmission app;
        if (request.getInquiryId() != null) {
            app = repository.findByInquiryId(request.getInquiryId()).orElse(null);
            if (app != null) {
                if (!EDITABLE.contains(app.getStatus())) {
                    throw new BadRequestException("Only draft or correction applications can be saved as draft");
                }
                mapRequest(request, app);
                ApplicationAdmission saved = repository.save(app);
                auditWriteService.record(AuditEventType.UPDATE, "APPLICATION_UPDATED", "APPLICATION",
                        String.valueOf(saved.getApplicationId()), "Application draft updated");
                return toResponse(saved);
            }
        }
        app = buildApplication(request);
        app.setStatus(ApplicationStatus.DRAFT);
        ApplicationAdmission saved = repository.save(app);
        auditWriteService.record(AuditEventType.CREATE, "APPLICATION_STARTED", "APPLICATION",
                String.valueOf(saved.getApplicationId()), "Application draft created");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse submit(ApplicationAdmissionRequest request) {
        requireManageApplications();
        if (request.getInquiryId() != null) {
            return repository.findByInquiryId(request.getInquiryId())
                    .map(existing -> submitExisting(existing.getApplicationId(), request))
                    .orElseGet(() -> submitNew(request));
        }
        return submitNew(request);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse submitExisting(Long applicationId, ApplicationAdmissionRequest request) {
        requireManageApplications();
        ApplicationAdmission app = getApplication(applicationId);
        if (!SUBMITTABLE.contains(app.getStatus())) {
            throw new BadRequestException("Only draft or correction applications can be submitted");
        }
        if (request != null && hasText(request.getApplicantName())) {
            mapRequest(request, app);
        }
        validateForSubmit(app);
        boolean resubmit = app.getStatus() == ApplicationStatus.ACTION_REQUIRED
                || app.getStatus() == ApplicationStatus.DOCUMENTS_PENDING;
        app.setStatus(ApplicationStatus.SUBMITTED);
        ApplicationAdmission saved = repository.save(app);
        markInquiry(saved.getInquiryId(), InquiryStatus.APPLICATION_SUBMITTED);
        auditWriteService.record(AuditEventType.STATE_CHANGE,
                resubmit ? "APPLICATION_RESUBMITTED" : "APPLICATION_SUBMITTED",
                "APPLICATION", String.valueOf(saved.getApplicationId()),
                resubmit ? "Application resubmitted after correction" : "Application submitted for review");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse update(Long applicationId, ApplicationAdmissionRequest request) {
        requireManageApplications();
        ApplicationAdmission app = getApplication(applicationId);
        if (!EDITABLE.contains(app.getStatus())) {
            throw new BadRequestException("Only DRAFT or ACTION_REQUIRED applications can be edited");
        }
        mapRequest(request, app);
        ApplicationAdmission saved = repository.save(app);
        auditWriteService.record(AuditEventType.UPDATE, "APPLICATION_UPDATED", "APPLICATION",
                String.valueOf(saved.getApplicationId()), "Application updated");
        return toResponse(saved);
    }

    @Override
    public ApplicationAdmissionResponse getById(Long applicationId) {
        requireViewApplications();
        return toResponse(getApplication(applicationId));
    }

    @Override
    public Page<ApplicationAdmissionResponse> getAll(Pageable pageable) {
        requireViewApplications();
        return repository.findByOrderByCreatedOnDesc(pageable).map(this::toResponse);
    }

    @Override
    public Page<ApplicationAdmissionResponse> getByStatus(ApplicationStatus status, Pageable pageable) {
        requireViewApplications();
        return repository.findByStatusOrderByCreatedOnDesc(status, pageable).map(this::toResponse);
    }

    @Override
    public Page<ApplicationAdmissionResponse> search(ApplicationSearchRequest request, Pageable pageable) {
        requireViewApplications();
        return repository.findAll(ApplicationAdmissionSpecification.filter(request), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse updateStatus(Long applicationId, ApplicationStatus status, String comments) {
        requireApproveApplications();
        ApplicationAdmission app = getApplication(applicationId);
        validateStatusChange(app.getStatus(), status);
        if (status == ApplicationStatus.APPROVED) {
            if (!REVIEWABLE.contains(app.getStatus())) {
                throw new BadRequestException("Application cannot be approved from status " + app.getStatus());
            }
            validateForApprove(app);
        }
        if (status == ApplicationStatus.ACTION_REQUIRED) {
            if (!REVIEWABLE.contains(app.getStatus()) && app.getStatus() != ApplicationStatus.SUBMITTED) {
                throw new BadRequestException("Correction can only be requested while the application is under review");
            }
            if (comments == null || comments.isBlank()) {
                throw new BadRequestException("A correction reason is required");
            }
        }
        app.setStatus(status);
        if (comments != null) {
            app.setInternalComments(comments);
        }
        if (status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED
                || status == ApplicationStatus.ACTION_REQUIRED) {
            stampReviewer(app);
        }
        ApplicationAdmission saved = repository.save(app);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "APPLICATION_STATUS_CHANGED", "APPLICATION",
                String.valueOf(saved.getApplicationId()), "Status → " + status);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse approve(Long applicationId, String comments) {
        requireApproveApplications();
        ApplicationAdmission app = getApplication(applicationId);
        if (!REVIEWABLE.contains(app.getStatus())) {
            throw new BadRequestException("Application cannot be approved from status " + app.getStatus());
        }
        validateForApprove(app);
        app.setStatus(ApplicationStatus.APPROVED);
        if (comments != null && !comments.isBlank()) {
            app.setInternalComments(comments.trim());
        }
        stampReviewer(app);
        ApplicationAdmission saved = repository.save(app);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "APPLICATION_APPROVED", "APPLICATION",
                String.valueOf(saved.getApplicationId()), "Admission approved");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse reject(Long applicationId, String comments) {
        requireApproveApplications();
        ApplicationAdmission app = getApplication(applicationId);
        if (app.getStatus() == ApplicationStatus.ENROLLED || app.getStatus() == ApplicationStatus.APPROVED) {
            throw new BadRequestException("Approved or enrolled applications cannot be rejected");
        }
        if (app.getStatus() == ApplicationStatus.REJECTED || app.getStatus() == ApplicationStatus.CANCELLED) {
            throw new BadRequestException("Application is already closed");
        }
        app.setStatus(ApplicationStatus.REJECTED);
        if (comments != null && !comments.isBlank()) {
            app.setInternalComments(comments.trim());
        }
        stampReviewer(app);
        ApplicationAdmission saved = repository.save(app);
        markInquiry(saved.getInquiryId(), InquiryStatus.LOST);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "APPLICATION_REJECTED", "APPLICATION",
                String.valueOf(saved.getApplicationId()), "Application rejected");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse requestCorrection(Long applicationId, String reason) {
        requireApproveApplications();
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("A correction reason is required");
        }
        ApplicationAdmission app = getApplication(applicationId);
        if (!REVIEWABLE.contains(app.getStatus()) && app.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new BadRequestException("Application cannot be sent back from status " + app.getStatus());
        }
        app.setStatus(ApplicationStatus.ACTION_REQUIRED);
        app.setInternalComments(reason.trim());
        stampReviewer(app);
        ApplicationAdmission saved = repository.save(app);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "APPLICATION_CORRECTION_REQUESTED", "APPLICATION",
                String.valueOf(saved.getApplicationId()), reason.trim());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyMatchResponse findFamilyMatch(String mobile, String email) {
        requireViewApplications();
        String normalizedMobile = digitsOnly(mobile);
        Parent parent = null;
        if (hasText(normalizedMobile)) {
            parent = parentRepository.findByMobileNumber(normalizedMobile).orElse(null);
        }
        if (parent == null && hasText(email)) {
            parent = parentRepository.findFirstByEmailIgnoreCase(email.trim()).orElse(null);
        }
        if (parent == null) {
            return FamilyMatchResponse.builder().matched(false).build();
        }
        List<FamilyMatchResponse.SiblingSummary> siblings = studentParentRepository
                .findByParent_ParentIdAndActiveTrue(parent.getParentId())
                .stream()
                .map(StudentParent::getStudent)
                .filter(s -> s != null)
                .map(student -> {
                    String className = studentEnrollmentRepository
                            .findActiveWithClassByStudentId(student.getStudentId())
                            .map(this::enrollmentClassLabel)
                            .orElse(null);
                    return FamilyMatchResponse.SiblingSummary.builder()
                            .studentId(student.getStudentId())
                            .studentName(studentFullName(student))
                            .studentCode(student.getStudentCode())
                            .className(className)
                            .build();
                })
                .collect(Collectors.toList());
        return FamilyMatchResponse.builder()
                .matched(true)
                .parentId(parent.getParentId())
                .parentName(parentFullName(parent))
                .mobileNumber(parent.getMobileNumber())
                .email(parent.getEmail())
                .students(siblings)
                .build();
    }

    @Override
    public ApplicationProgressResponse getProgress(Long applicationId) {
        ApplicationAdmission app = getApplication(applicationId);
        int totalSteps = 7;
        int completed = 0;
        if (hasText(app.getApplicantName()) && hasText(app.getApplyingForClass()) && hasText(app.getContactNumber())) {
            completed++;
        }
        if (hasText(app.getParentName()) && hasText(app.getParentContact())) {
            completed++;
        }
        if (hasText(app.getAddress())) {
            completed++;
        }
        if (app.getAcademicYearId() != null && app.getClassId() != null) {
            completed++;
        }
        if (!listDocuments(applicationId).isEmpty()
                || (app.getUploadedDocuments() != null && !app.getUploadedDocuments().isEmpty())) {
            completed++;
        }
        if (app.getFeeStatus() == FeePaymentStatus.PAID || app.getFeeAmount() != null) {
            completed++;
        }
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            completed++;
        }
        int percent = (int) Math.round((completed * 100.0) / totalSteps);
        return ApplicationProgressResponse.builder()
                .applicationId(app.getApplicationId())
                .applicationNumber(app.getApplicationNumber())
                .status(app.getStatus())
                .totalSteps(totalSteps)
                .completedSteps(completed)
                .completionPercent(percent)
                .build();
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse archive(Long applicationId) {
        ApplicationAdmission app = getApplication(applicationId);
        if (app.getStatus() != ApplicationStatus.DRAFT && app.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new BadRequestException("Only DRAFT or SUBMITTED applications can be archived");
        }
        if (app.isArchived()) {
            throw new BadRequestException("Application is already archived");
        }
        app.setArchived(true);
        return toResponse(repository.save(app));
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse unarchive(Long applicationId) {
        ApplicationAdmission app = getApplication(applicationId);
        if (!app.isArchived()) {
            throw new BadRequestException("Application is not archived");
        }
        app.setArchived(false);
        return toResponse(repository.save(app));
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse recordFee(Long applicationId, RecordFeeRequest request) {
        ApplicationAdmission app = getApplication(applicationId);
        if (app.getStatus() == ApplicationStatus.REJECTED || app.getStatus() == ApplicationStatus.CANCELLED) {
            throw new BadRequestException("Cannot record a fee on a closed application");
        }
        app.setFeeAmount(request.getAmount());
        app.setFeeReceiptNumber(request.getReceiptNumber());
        app.setFeePaymentMode(request.getPaymentMode());
        app.setFeePaidOn(request.getPaidOn() != null ? request.getPaidOn() : LocalDate.now());
        app.setFeeReceivedBy(hasText(request.getReceivedBy()) ? request.getReceivedBy() : currentUsername());
        app.setFeeRemarks(request.getRemarks());
        app.setFeeStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : FeePaymentStatus.PAID);
        if (app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.DOCUMENTS_PENDING) {
            app.setStatus(ApplicationStatus.FEE_PENDING);
        }
        if (app.getFeeStatus() == FeePaymentStatus.PAID && app.getStatus() == ApplicationStatus.FEE_PENDING) {
            app.setStatus(ApplicationStatus.UNDER_REVIEW);
        }
        return toResponse(repository.save(app));
    }

    @Override
    @Transactional
    public EnrollmentResultResponse enroll(Long applicationId, EnrollApplicationRequest request) {
        requireApproveApplications();
        ApplicationAdmission app = getApplication(applicationId);
        if (app.getStatus() != ApplicationStatus.APPROVED) {
            throw new BadRequestException("Only approved applications can be enrolled");
        }
        if (app.getStudentId() != null) {
            throw new BadRequestException("This application is already enrolled");
        }
        if (request.getAcademicYearId() == null || request.getClassId() == null) {
            throw new BadRequestException("Academic year and class are required");
        }
        validateForEnroll(app);

        String className = lookupName(academicsLookupService.getClassesByYear(request.getAcademicYearId()), request.getClassId());
        if (className == null) {
            throw new BadRequestException("Class was not found for the selected academic year");
        }
        if (request.getSectionId() != null) {
            String sectionName = lookupName(academicsLookupService.getSectionsByClass(request.getClassId()), request.getSectionId());
            if (sectionName == null) {
                throw new BadRequestException("Section was not found for the selected class");
            }
        }

        String admissionNumber = generateAdmissionNumber();
        StudentCreateRequest studentRequest = toStudentCreateRequest(app, request, admissionNumber);
        StudentResponseDTO student;
        try {
            student = studentService.createStudent(studentRequest);
        } catch (IOException ex) {
            throw new BadRequestException("Could not create the student record");
        }

        app.setAcademicYearId(request.getAcademicYearId());
        app.setClassId(request.getClassId());
        app.setSectionId(request.getSectionId());
        app.setApplyingForClass(className);
        app.setStudentId(student.getStudentId());
        app.setStatus(ApplicationStatus.ENROLLED);
        repository.save(app);
        markInquiry(app.getInquiryId(), InquiryStatus.APPLICATION_SUBMITTED);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "ENROLLMENT_COMPLETED", "APPLICATION",
                String.valueOf(app.getApplicationId()),
                "Student enrolled: " + student.getStudentId());

        return EnrollmentResultResponse.builder()
                .applicationId(app.getApplicationId())
                .applicationNumber(app.getApplicationNumber())
                .studentId(student.getStudentId())
                .studentCode(student.getStudentCode())
                .admissionNumber(student.getAdmissionNumber())
                .studentName(student.getFullName())
                .academicYearId(request.getAcademicYearId())
                .classId(request.getClassId())
                .sectionId(request.getSectionId())
                .build();
    }

    @Override
    @Transactional
    public ApplicationDocumentResponse uploadDocument(
            Long applicationId, MultipartFile file, String documentType, String remarks) {
        requireManageApplications();
        ApplicationAdmission app = getApplication(applicationId);
        if (app.getStatus() == ApplicationStatus.ENROLLED || app.getStatus() == ApplicationStatus.REJECTED
                || app.getStatus() == ApplicationStatus.CANCELLED) {
            throw new BadRequestException("Documents cannot be uploaded for this application status");
        }
        try {
            String path = fileStorageService.store(file, "adm_" + applicationId);
            AdmissionApplicationDocument doc = new AdmissionApplicationDocument();
            doc.setApplication(app);
            doc.setDocumentType(hasText(documentType) ? documentType.trim() : "OTHER");
            doc.setOriginalName(file.getOriginalFilename());
            doc.setStoredPath(path);
            doc.setStatus(DocumentCheckStatus.PENDING);
            if (hasText(remarks)) {
                doc.setRemarks(remarks.trim());
            }
            ApplicationDocumentResponse response = toDocumentResponse(documentRepository.save(doc));
            auditWriteService.record(AuditEventType.UPDATE, "DOCUMENT_UPLOADED", "APPLICATION",
                    String.valueOf(applicationId), "Document uploaded: " + response.getDocumentType());
            return response;
        } catch (IOException ex) {
            throw new BadRequestException("Could not store the document");
        }
    }

    @Override
    public List<ApplicationDocumentResponse> listDocuments(Long applicationId) {
        getApplication(applicationId);
        return documentRepository.findByApplicationApplicationIdOrderByCreatedOnDesc(applicationId)
                .stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApplicationDocumentResponse updateDocumentStatus(Long documentId, DocumentCheckStatus status, String remarks) {
        requireApproveApplications();
        AdmissionApplicationDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        doc.setStatus(status);
        doc.setRemarks(remarks);
        ApplicationDocumentResponse response = toDocumentResponse(documentRepository.save(doc));
        String event = status == DocumentCheckStatus.VERIFIED ? "DOCUMENT_VERIFIED" : "DOCUMENT_REJECTED";
        auditWriteService.record(AuditEventType.UPDATE, event, "APPLICATION",
                String.valueOf(doc.getApplication().getApplicationId()),
                doc.getDocumentType() + (remarks != null ? ": " + remarks : ""));
        return response;
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        requireManageApplications();
        AdmissionApplicationDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        String storedPath = doc.getStoredPath();
        documentRepository.delete(doc);
        fileStorageService.deleteQuietly(storedPath);
    }

    @Override
    public ApplicationDocumentFile downloadDocument(Long documentId) {
        requireViewApplications();
        AdmissionApplicationDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        String originalName = StringUtils.hasText(doc.getOriginalName())
                ? doc.getOriginalName()
                : "document-" + documentId;
        MediaType mediaType = MediaTypeFactory.getMediaType(originalName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return new ApplicationDocumentFile(
                fileStorageService.loadAsResource(doc.getStoredPath()),
                originalName,
                mediaType.toString()
        );
    }

    private ApplicationAdmissionResponse submitNew(ApplicationAdmissionRequest request) {
        ApplicationAdmission app = buildApplication(request);
        validateForSubmit(app);
        app.setStatus(ApplicationStatus.SUBMITTED);
        ApplicationAdmission saved = repository.save(app);
        markInquiry(saved.getInquiryId(), InquiryStatus.APPLICATION_SUBMITTED);
        return toResponse(saved);
    }

    private ApplicationAdmission buildApplication(ApplicationAdmissionRequest request) {
        ApplicationAdmission app = new ApplicationAdmission();
        app.setApplicationNumber(generateApplicationNumber());
        mapRequest(request, app);
        return app;
    }

    private void mapRequest(ApplicationAdmissionRequest request, ApplicationAdmission app) {
        app.setInquiryId(request.getInquiryId());
        if (hasText(request.getApplicantName())) {
            app.setApplicantName(request.getApplicantName().trim());
        } else if (!hasText(app.getApplicantName())) {
            app.setApplicantName("Draft Applicant");
        }
        if (request.getDateOfBirth() != null) {
            app.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            app.setGender(request.getGender());
        }
        app.setApplyingForClass(resolveClassName(request));
        if (request.getAcademicYearId() != null) {
            app.setAcademicYearId(request.getAcademicYearId());
        }
        if (request.getClassId() != null) {
            app.setClassId(request.getClassId());
        }
        if (request.getSectionId() != null) {
            app.setSectionId(request.getSectionId());
        }
        if (request.getEmail() != null) {
            app.setEmail(request.getEmail());
        }
        if (request.getContactNumber() != null) {
            app.setContactNumber(request.getContactNumber());
        }
        if (request.getAddress() != null) {
            app.setAddress(request.getAddress());
        }
        if (request.getParentName() != null) {
            app.setParentName(request.getParentName());
        }
        if (request.getParentContact() != null) {
            app.setParentContact(request.getParentContact());
        }
        if (request.getParentEmail() != null) {
            app.setParentEmail(request.getParentEmail());
        }
        if (request.getInternalComments() != null) {
            app.setInternalComments(request.getInternalComments());
        }
        if (request.getProfile() != null) {
            app.setProfileDetails(writeProfile(request.getProfile()));
        }
    }

    private String resolveClassName(ApplicationAdmissionRequest request) {
        if (request.getClassId() != null && request.getAcademicYearId() != null) {
            String name = lookupName(academicsLookupService.getClassesByYear(request.getAcademicYearId()), request.getClassId());
            if (name != null) {
                return name;
            }
        }
        return request.getApplyingForClass();
    }

    private ApplicationAdmission getApplication(Long applicationId) {
        return repository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }

    private void validateStatusChange(ApplicationStatus current, ApplicationStatus target) {
        if (target == null) {
            throw new BadRequestException("Target status is required");
        }
        if (current == ApplicationStatus.ENROLLED) {
            throw new BadRequestException("Enrolled applications cannot be modified");
        }
        if (current == ApplicationStatus.REJECTED || current == ApplicationStatus.CANCELLED) {
            throw new BadRequestException("Application is closed and cannot be modified");
        }
        if (current == ApplicationStatus.APPROVED && target != ApplicationStatus.ENROLLED) {
            throw new BadRequestException("Approved applications can only be enrolled");
        }
        if (target == ApplicationStatus.ENROLLED) {
            throw new BadRequestException("Use the enroll action to create the student record");
        }
    }

    private void validateForSubmit(ApplicationAdmission app) {
        if (!hasText(app.getApplicantName())) {
            throw new BadRequestException("Applicant name is required");
        }
        if (!hasText(app.getApplyingForClass()) && app.getClassId() == null) {
            throw new BadRequestException("Class is required");
        }
        if (!hasText(app.getContactNumber())) {
            throw new BadRequestException("Contact number is required");
        }
        if (!hasText(app.getParentName()) || !hasText(app.getParentContact())) {
            throw new BadRequestException("Parent name and contact are required");
        }
    }

    private void validateForApprove(ApplicationAdmission app) {
        validateForSubmit(app);
        if (app.getDateOfBirth() == null) {
            throw new BadRequestException("Date of birth is required before approval");
        }
        ApplicationProfileDetails profile = readProfile(app.getProfileDetails());
        List<String> required = RequiredDocumentsResolver.resolve(
                settingService.requiredDocuments(),
                app.getApplyingForClass(),
                profile != null ? profile.getHasPreviousSchooling() : null,
                profile != null ? profile.getTcNumber() : null,
                profile != null ? profile.getPreviousSchoolName() : null);
        if (required.isEmpty()) {
            return;
        }
        List<ApplicationDocumentResponse> docs = listDocuments(app.getApplicationId());
        for (String type : required) {
            boolean verified = docs.stream().anyMatch(doc ->
                    type.equalsIgnoreCase(doc.getDocumentType())
                            && doc.getStatus() == DocumentCheckStatus.VERIFIED);
            if (!verified) {
                throw new BadRequestException("Required document is missing or not verified: "
                        + type.replace('_', ' ').toLowerCase());
            }
        }
    }

    private void validateForEnroll(ApplicationAdmission app) {
        validateForApprove(app);
        if (app.getDateOfBirth() == null) {
            throw new BadRequestException("Date of birth is required to create the student");
        }
        if (!hasText(app.getParentName()) || !hasText(app.getParentContact())) {
            throw new BadRequestException("Parent details are required to create the student");
        }
    }

    private StudentCreateRequest toStudentCreateRequest(
            ApplicationAdmission app, EnrollApplicationRequest enroll, String admissionNumber) {
        String[] studentNames = splitName(app.getApplicantName());
        String[] parentNames = splitName(hasText(app.getParentName()) ? app.getParentName() : "Guardian " + studentNames[1]);

        StudentCreateRequest request = new StudentCreateRequest();
        request.setAdmissionNumber(admissionNumber);
        request.setFirstName(studentNames[0]);
        request.setLastName(studentNames[1]);
        request.setGender(hasText(app.getGender()) ? app.getGender() : "OTHER");
        request.setDateOfBirth(app.getDateOfBirth());
        request.setMobileNumber(digitsOnly(app.getContactNumber()));
        request.setEmail(app.getEmail());
        request.setRemarks(app.getInternalComments());
        request.setParentFirstName(parentNames[0]);
        request.setParentLastName(parentNames[1]);
        request.setParentMobileNumber(digitsOnly(app.getParentContact()));
        if (hasText(app.getParentEmail())
                && (!hasText(app.getEmail()) || !app.getParentEmail().equalsIgnoreCase(app.getEmail()))) {
            request.setParentEmail(app.getParentEmail());
        }
        request.setAcademicYearId(enroll.getAcademicYearId());
        request.setClassId(enroll.getClassId());
        request.setSectionId(enroll.getSectionId());
        ApplicationProfileDetails profile = readProfile(app.getProfileDetails());
        if (profile != null && profile.getLinkedParentId() != null) {
            request.setExistingParentId(profile.getLinkedParentId());
        }
        if (profile != null) {
            request.setBloodGroup(profile.getBloodGroup());
            request.setReligion(profile.getReligion());
            request.setNationality(profile.getNationality());
            request.setMotherTongue(profile.getMotherTongue());
            request.setParentOccupation(profile.getFatherOccupation());
        }
        return request;
    }

    private void stampReviewer(ApplicationAdmission app) {
        app.setReviewedOn(LocalDate.now());
        app.setReviewedByUserId(currentUserId());
    }

    private void markInquiry(Long inquiryId, InquiryStatus status) {
        if (inquiryId == null) {
            return;
        }
        inquiryRepository.findByInquiryIdAndDeletedFalse(inquiryId).ifPresent(inquiry -> {
            inquiry.setStatus(status);
            inquiryRepository.save(inquiry);
        });
    }

    private String generateApplicationNumber() {
        String prefix = settingService.applicationPrefix();
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String candidate = prefix + "-" + yearMonth + "-" + ThreadLocalRandom.current().nextLong(10000, 99999);
        while (repository.existsByApplicationNumber(candidate)) {
            candidate = prefix + "-" + yearMonth + "-" + ThreadLocalRandom.current().nextLong(10000, 99999);
        }
        return candidate;
    }

    private String generateAdmissionNumber() {
        String prefix = settingService.admissionPrefix();
        String year = String.valueOf(LocalDate.now().getYear());
        String candidate;
        do {
            candidate = prefix + "-" + year + "-" + ThreadLocalRandom.current().nextLong(10000, 99999);
        } while (studentRepository.existsByAdmissionNumber(candidate));
        return candidate;
    }

    private ApplicationAdmissionResponse toResponse(ApplicationAdmission a) {
        List<ApplicationDocumentResponse> docs = documentRepository
                .findByApplicationApplicationIdOrderByCreatedOnDesc(a.getApplicationId())
                .stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
        return ApplicationAdmissionResponse.builder()
                .applicationId(a.getApplicationId())
                .applicationNumber(a.getApplicationNumber())
                .inquiryId(a.getInquiryId())
                .applicantName(a.getApplicantName())
                .dateOfBirth(a.getDateOfBirth())
                .gender(a.getGender())
                .applyingForClass(a.getApplyingForClass())
                .academicYearId(a.getAcademicYearId())
                .classId(a.getClassId())
                .sectionId(a.getSectionId())
                .studentId(a.getStudentId())
                .email(a.getEmail())
                .contactNumber(a.getContactNumber())
                .address(a.getAddress())
                .parentName(a.getParentName())
                .parentContact(a.getParentContact())
                .parentEmail(a.getParentEmail())
                .profile(readProfile(a.getProfileDetails()))
                .status(a.getStatus())
                .internalComments(a.getInternalComments())
                .uploadedDocuments(copiedDocumentUrls(a))
                .documents(docs)
                .feeAmount(a.getFeeAmount())
                .feeReceiptNumber(a.getFeeReceiptNumber())
                .feePaymentMode(a.getFeePaymentMode())
                .feePaidOn(a.getFeePaidOn())
                .feeReceivedBy(a.getFeeReceivedBy())
                .feeRemarks(a.getFeeRemarks())
                .feeStatus(a.getFeeStatus())
                .reviewedByUserId(a.getReviewedByUserId())
                .reviewedOn(a.getReviewedOn())
                .archived(a.isArchived())
                .createdOn(a.getCreatedOn())
                .createdBy(a.getCreatedBy())
                .build();
    }

    private List<String> copiedDocumentUrls(ApplicationAdmission a) {
        try {
            List<String> docs = a.getUploadedDocuments();
            return docs == null || docs.isEmpty() ? List.of() : List.copyOf(docs);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private ApplicationDocumentResponse toDocumentResponse(AdmissionApplicationDocument doc) {
        return ApplicationDocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .applicationId(doc.getApplication().getApplicationId())
                .documentType(doc.getDocumentType())
                .originalName(doc.getOriginalName())
                .status(doc.getStatus())
                .remarks(doc.getRemarks())
                .createdOn(doc.getCreatedOn())
                .createdBy(doc.getCreatedBy())
                .build();
    }

    private static String lookupName(List<LookupDTO> items, Long id) {
        if (items == null || id == null) {
            return null;
        }
        return items.stream()
                .filter(item -> id.equals(item.getId()))
                .map(LookupDTO::getName)
                .findFirst()
                .orElse(null);
    }

    private static String[] splitName(String fullName) {
        String value = fullName == null ? "" : fullName.trim();
        if (value.isEmpty()) {
            return new String[] {"Student", "Applicant"};
        }
        String[] parts = value.split("\\s+", 2);
        if (parts.length == 1) {
            return new String[] {parts[0], parts[0]};
        }
        return parts;
    }

    private Long currentUserId() {
        String username = currentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .map(User::getId)
                .orElse(null);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private boolean hasElevatedRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "SUPER_ADMIN".equals(a)
                        || "ORGANIZATION_OWNER".equals(a)
                        || "ORGANIZATION_ADMIN".equals(a));
    }

    private void requireViewApplications() {
        requireApplicationPrivilege("VIEW");
    }

    private void requireManageApplications() {
        requireApplicationPrivilege("MANAGE");
    }

    private void requireApproveApplications() {
        requireApplicationPrivilege("APPROVE");
    }

    private void requireApplicationPrivilege(String privilege) {
        if (hasElevatedRole()) {
            return;
        }
        Long userId = currentUserId();
        Long orgId = OrganizationContext.getOrganizationId();
        if (userId == null || orgId == null
                || !permissionService.hasPermission(userId, orgId, RESOURCE_ADMISSIONS_APPLICATIONS, privilege)) {
            throw new AccessDeniedException("ADMISSIONS_APPLICATIONS:" + privilege + " required");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String enrollmentClassLabel(StudentEnrollment enrollment) {
        if (enrollment == null || enrollment.getClassEntity() == null) {
            return null;
        }
        String name = enrollment.getClassEntity().getClassName();
        if (enrollment.getSection() != null && hasText(enrollment.getSection().getName())) {
            return name + " · " + enrollment.getSection().getName();
        }
        return name;
    }

    private static String studentFullName(Student student) {
        if (student == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (hasText(student.getFirstName())) {
            sb.append(student.getFirstName().trim());
        }
        if (hasText(student.getMiddleName())) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(student.getMiddleName().trim());
        }
        if (hasText(student.getLastName())) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(student.getLastName().trim());
        }
        return sb.toString();
    }

    private static String parentFullName(Parent parent) {
        if (parent == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (hasText(parent.getFirstName())) {
            sb.append(parent.getFirstName().trim());
        }
        if (hasText(parent.getMiddleName())) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(parent.getMiddleName().trim());
        }
        if (hasText(parent.getLastName())) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(parent.getLastName().trim());
        }
        return sb.toString();
    }

    private String writeProfile(ApplicationProfileDetails profile) {
        if (profile == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Could not save application profile details");
        }
    }

    private ApplicationProfileDetails readProfile(String json) {
        if (!hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ApplicationProfileDetails.class);
        } catch (JsonProcessingException ex) {
            log.warn("Could not parse application profile_details: {}", ex.getMessage());
            return null;
        }
    }

    private static String digitsOnly(String value) {
        if (!hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits.isEmpty() ? null : digits;
    }
}
