package com.thinkerscave.admission.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InquiryFullDetailResponse {

    private InquiryResponse inquiry;
    private List<FollowUpResponse> followUps;
    private List<CounselingNoteResponse> counselingNotes;
    private List<InquiryTimelineItemResponse> timeline;
    private Long applicationId;
    private String applicationNumber;
    private String applicationStatus;
    private Long studentId;
    private String studentCode;
    private String admissionNumber;
}