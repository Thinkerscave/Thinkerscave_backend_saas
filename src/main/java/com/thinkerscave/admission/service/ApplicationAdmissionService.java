package com.thinkerscave.admission.service;

import com.thinkerscave.admission.dto.request.ApplicationAdmissionRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationAdmissionService {

    ApplicationAdmissionResponse saveDraft(ApplicationAdmissionRequest request);

    ApplicationAdmissionResponse submit(ApplicationAdmissionRequest request);

    ApplicationAdmissionResponse update(Long applicationId, ApplicationAdmissionRequest request);

    ApplicationAdmissionResponse getById(Long applicationId);

    Page<ApplicationAdmissionResponse> getAll(Pageable pageable);

    Page<ApplicationAdmissionResponse> getByStatus(ApplicationStatus status, Pageable pageable);

    ApplicationAdmissionResponse updateStatus(Long applicationId, ApplicationStatus status, String comments);
}
