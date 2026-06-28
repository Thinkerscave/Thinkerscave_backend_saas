package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.ProvisionOrganizationRequest;
import com.thinkerscave.platform.dto.response.ProvisioningJobResponse;
import com.thinkerscave.platform.dto.response.ProvisioningResultResponse;
import com.thinkerscave.platform.enums.ProvisionJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProvisionService {

    ProvisioningResultResponse provision(ProvisionOrganizationRequest request);

    Page<ProvisioningJobResponse> getJobs(ProvisionJobStatus status, String search, Pageable pageable);

    ProvisioningJobResponse getJobById(Long id);

    ProvisioningJobResponse getJobLogs(Long id);

    ProvisioningJobResponse retryJob(Long id);
}
