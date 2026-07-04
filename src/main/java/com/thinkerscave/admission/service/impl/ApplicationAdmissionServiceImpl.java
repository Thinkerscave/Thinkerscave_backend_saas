package com.thinkerscave.admission.service.impl;

import com.thinkerscave.admission.dto.request.ApplicationAdmissionRequest;
import com.thinkerscave.admission.dto.request.ApplicationSearchRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.dto.response.ApplicationProgressResponse;
import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.service.ApplicationAdmissionService;
import com.thinkerscave.admission.specification.ApplicationAdmissionSpecification;
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
    public Page<ApplicationAdmissionResponse> search(ApplicationSearchRequest request, Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        return repository.findAll(ApplicationAdmissionSpecification.filter(orgId, request), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse updateStatus(Long applicationId, ApplicationStatus status, String comments) {
        ApplicationAdmission app = getApplication(applicationId);
        validateStatusChange(app.getStatus(), status);
        app.setStatus(status);
        if (comments != null) {
            app.setInternalComments(comments);
        }
        if (status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED) {
            app.setReviewedOn(LocalDate.now());
        }
        return toResponse(repository.save(app));
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse approve(Long applicationId, String comments) {
        return updateStatus(applicationId, ApplicationStatus.APPROVED, comments);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse reject(Long applicationId, String comments) {
        return updateStatus(applicationId, ApplicationStatus.REJECTED, comments);
    }

    @Override
    public ApplicationProgressResponse getProgress(Long applicationId) {
        ApplicationAdmission app = getApplication(applicationId);
        int totalSteps = 6;
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
        if (app.getUploadedDocuments() != null && !app.getUploadedDocuments().isEmpty()) {
            completed++;
        }
        if (hasText(app.getInternalComments())) {
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

    private void validateStatusChange(ApplicationStatus current, ApplicationStatus target) {
        if (current == ApplicationStatus.APPROVED || current == ApplicationStatus.ENROLLED) {
            throw new IllegalStateException("Application cannot be modified after approval");
        }
        if (current == ApplicationStatus.REJECTED || current == ApplicationStatus.CANCELLED) {
            throw new IllegalStateException("Application is closed and cannot be modified");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target status is required");
        }
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
