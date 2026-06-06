package com.thinkerscave.common.admission.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Aggregated response for the Inquiry Detail Workspace.
 * Bundles every tab's data into a single payload so the UI can hydrate without N requests.
 */
@Getter
@Setter
@Builder
public class InquiryFullDetailResponse {
    private InquiryResponse overview;
    private List<FollowUpResponse> followUps;
    private List<CounselingNoteResponse> counselingNotes;
    private List<String> uploadedDocuments;
    private List<String> missingDocuments;
    private List<InquiryTimelineEntry> timeline;
}
