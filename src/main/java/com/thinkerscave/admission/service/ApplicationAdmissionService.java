package com.thinkerscave.admission.service;

import com.thinkerscave.admission.dto.request.ApplicationAdmissionRequest;
import com.thinkerscave.admission.dto.request.ApplicationSearchRequest;
import com.thinkerscave.admission.dto.request.EnrollApplicationRequest;
import com.thinkerscave.admission.dto.request.RecordFeeRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.dto.response.ApplicationDocumentResponse;
import com.thinkerscave.admission.dto.response.ApplicationProgressResponse;
import com.thinkerscave.admission.dto.response.EnrollmentResultResponse;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.DocumentCheckStatus;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ApplicationAdmissionService {

    ApplicationAdmissionResponse saveDraft(ApplicationAdmissionRequest request);

    ApplicationAdmissionResponse submit(ApplicationAdmissionRequest request);

    ApplicationAdmissionResponse submitExisting(Long applicationId, ApplicationAdmissionRequest request);

    ApplicationAdmissionResponse update(Long applicationId, ApplicationAdmissionRequest request);

    ApplicationAdmissionResponse getById(Long applicationId);

    Page<ApplicationAdmissionResponse> getAll(Pageable pageable);

    Page<ApplicationAdmissionResponse> getByStatus(ApplicationStatus status, Pageable pageable);

    Page<ApplicationAdmissionResponse> search(ApplicationSearchRequest request, Pageable pageable);

    ApplicationAdmissionResponse updateStatus(Long applicationId, ApplicationStatus status, String comments);

    ApplicationAdmissionResponse approve(Long applicationId, String comments);

    ApplicationAdmissionResponse reject(Long applicationId, String comments);

    ApplicationProgressResponse getProgress(Long applicationId);

    ApplicationAdmissionResponse archive(Long applicationId);

    ApplicationAdmissionResponse unarchive(Long applicationId);

    ApplicationAdmissionResponse recordFee(Long applicationId, RecordFeeRequest request);

    EnrollmentResultResponse enroll(Long applicationId, EnrollApplicationRequest request);

    ApplicationDocumentResponse uploadDocument(Long applicationId, MultipartFile file, String documentType);

    List<ApplicationDocumentResponse> listDocuments(Long applicationId);

    ApplicationDocumentResponse updateDocumentStatus(Long documentId, DocumentCheckStatus status, String remarks);

    void deleteDocument(Long documentId);

    Resource downloadDocument(Long documentId);
}
