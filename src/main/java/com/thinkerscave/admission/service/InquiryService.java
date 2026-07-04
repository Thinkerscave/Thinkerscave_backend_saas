package com.thinkerscave.admission.service;

import com.thinkerscave.admission.dto.request.CounselingNoteRequest;
import com.thinkerscave.admission.dto.request.FollowUpRequest;
import com.thinkerscave.admission.dto.request.InquiryRequest;
import com.thinkerscave.admission.dto.request.LeadSearchRequest;
import com.thinkerscave.admission.dto.response.AdmissionKpiResponse;
import com.thinkerscave.admission.dto.response.CounselingNoteResponse;
import com.thinkerscave.admission.dto.response.FollowUpResponse;
import com.thinkerscave.admission.dto.response.InquiryFullDetailResponse;
import com.thinkerscave.admission.dto.response.InquiryQuickActionResponse;
import com.thinkerscave.admission.dto.response.InquiryResponse;
import com.thinkerscave.admission.dto.response.InquiryTimelineItemResponse;
import com.thinkerscave.admission.dto.response.InquiryWorkspaceKpiResponse;
import com.thinkerscave.admission.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InquiryService {

    InquiryResponse create(InquiryRequest request);

    InquiryResponse update(Long inquiryId, InquiryRequest request);

    InquiryResponse getById(Long inquiryId);

    Page<InquiryResponse> getAll(Pageable pageable);

    Page<InquiryResponse> search(LeadSearchRequest request, Pageable pageable);

    List<InquiryResponse> getByStatus(InquiryStatus status);

    List<InquiryResponse> getPendingFollowUps();

    void softDelete(Long inquiryId);

    InquiryResponse updateStatus(Long inquiryId, InquiryStatus newStatus);

    InquiryResponse markLost(Long inquiryId, String reason);

    InquiryResponse assignCounselor(Long inquiryId, Long counselorId);

    com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse convertToApplication(Long inquiryId);

    FollowUpResponse addFollowUp(Long inquiryId, FollowUpRequest request);

    List<FollowUpResponse> getFollowUps(Long inquiryId);

    List<FollowUpResponse> getTodayFollowUps();

    List<FollowUpResponse> getOverdueFollowUps();

    FollowUpResponse updateFollowUp(Long followUpId, FollowUpRequest request);

    FollowUpResponse completeFollowUp(Long followUpId);

    CounselingNoteResponse addCounselingNote(Long inquiryId, CounselingNoteRequest request);

    List<CounselingNoteResponse> getCounselingNotes(Long inquiryId);

    AdmissionKpiResponse getKpi();

    InquiryWorkspaceKpiResponse getWorkspaceKpi();

    InquiryQuickActionResponse getQuickActions();

    InquiryFullDetailResponse getFullDetail(Long inquiryId);

    List<InquiryTimelineItemResponse> getTimeline(Long inquiryId);
}
