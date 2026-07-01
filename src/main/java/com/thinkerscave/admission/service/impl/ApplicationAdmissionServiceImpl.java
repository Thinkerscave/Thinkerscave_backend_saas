package com.thinkerscave.admission.service.impl;

import com.thinkerscave.admission.dto.request.ApplicationAdmissionRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.service.ApplicationAdmissionService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApplicationAdmissionServiceImpl implements ApplicationAdmissionService {

    private final ApplicationAdmissionRepository repository;

    @Override
    @Transactional
    public ApplicationAdmissionResponse saveDraft(ApplicationAdmissionRequest request) {
        ApplicationAdmission app = buildApplication(request);
        app.setStatus(ApplicationStatus.DRAFT);
        return toResponse(repository.save(app));
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse submit(ApplicationAdmissionRequest request) {
        ApplicationAdmission app = buildApplication(request);
        app.setStatus(ApplicationStatus.SUBMITTED);
        return toResponse(repository.save(app));
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse update(Long applicationId, ApplicationAdmissionRequest request) {
        ApplicationAdmission app = getApplication(applicationId);
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT applications can be edited");
        }
        mapRequest(request, app);
        return toResponse(repository.save(app));
    }

    @Override
    public ApplicationAdmissionResponse getById(Long applicationId) {
        return toResponse(getApplication(applicationId));
    }

    @Override
    public Page<ApplicationAdmissionResponse> getAll(Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        return repository.findByOrganizationIdOrderByCreatedOnDesc(orgId, pageable).map(this::toResponse);
    }

    @Override
    public Page<ApplicationAdmissionResponse> getByStatus(ApplicationStatus status, Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        return repository.findByOrganizationIdAndStatusOrderByCreatedOnDesc(orgId, status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse updateStatus(Long applicationId, ApplicationStatus status, String comments) {
        ApplicationAdmission app = getApplication(applicationId);
        app.setStatus(status);
        if (comments != null) {
            app.setInternalComments(comments);
        }
        if (status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED) {
            app.setReviewedOn(LocalDate.now());
        }
        return toResponse(repository.save(app));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ApplicationAdmission buildApplication(ApplicationAdmissionRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        ApplicationAdmission app = new ApplicationAdmission();
        app.setOrganizationId(orgId);
        app.setApplicationNumber(generateApplicationNumber());
        mapRequest(request, app);
        return app;
    }

    private void mapRequest(ApplicationAdmissionRequest request, ApplicationAdmission app) {
        app.setInquiryId(request.getInquiryId());
        app.setApplicantName(request.getApplicantName());
        app.setDateOfBirth(request.getDateOfBirth());
        app.setGender(request.getGender());
        app.setApplyingForClass(request.getApplyingForClass());
        app.setEmail(request.getEmail());
        app.setContactNumber(request.getContactNumber());
        app.setAddress(request.getAddress());
        app.setParentName(request.getParentName());
        app.setParentContact(request.getParentContact());
        app.setParentEmail(request.getParentEmail());
        app.setInternalComments(request.getInternalComments());
    }

    private ApplicationAdmission getApplication(Long applicationId) {
        Long orgId = OrganizationContext.getOrganizationId();
        ApplicationAdmission app = repository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        if (!orgId.equals(app.getOrganizationId())) {
            throw new ResourceNotFoundException("Application not found: " + applicationId);
        }
        return app;
    }

    private String generateApplicationNumber() {
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long suffix = ThreadLocalRandom.current().nextLong(10000, 99999);
        String candidate = "APP-" + yearMonth + "-" + suffix;
        while (repository.existsByApplicationNumber(candidate)) {
            candidate = "APP-" + yearMonth + "-" + ThreadLocalRandom.current().nextLong(10000, 99999);
        }
        return candidate;
    }

    private ApplicationAdmissionResponse toResponse(ApplicationAdmission a) {
        return ApplicationAdmissionResponse.builder()
                .applicationId(a.getApplicationId())
                .applicationNumber(a.getApplicationNumber())
                .inquiryId(a.getInquiryId())
                .applicantName(a.getApplicantName())
                .dateOfBirth(a.getDateOfBirth())
                .gender(a.getGender())
                .applyingForClass(a.getApplyingForClass())
                .email(a.getEmail())
                .contactNumber(a.getContactNumber())
                .address(a.getAddress())
                .parentName(a.getParentName())
                .parentContact(a.getParentContact())
                .parentEmail(a.getParentEmail())
                .status(a.getStatus())
                .internalComments(a.getInternalComments())
                .uploadedDocuments(a.getUploadedDocuments())
                .createdOn(a.getCreatedOn())
                .createdBy(a.getCreatedBy())
                .build();
    }
}
