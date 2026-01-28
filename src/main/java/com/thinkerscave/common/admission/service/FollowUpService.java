package com.thinkerscave.common.admission.service;

import com.thinkerscave.common.admission.dto.FollowUpRequest;
import com.thinkerscave.common.admission.dto.FollowUpResponse;
import java.util.List;

public interface FollowUpService {
    FollowUpResponse addFollowUp(Long inquiryId, FollowUpRequest request);
    List<FollowUpResponse> getFollowUps(Long inquiryId);
}
