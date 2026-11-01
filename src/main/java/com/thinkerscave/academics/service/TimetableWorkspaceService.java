package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.TimetableEntryRequest;
import com.thinkerscave.academics.dto.response.TimetableCellResponse;
import com.thinkerscave.academics.dto.response.TimetableConflictResponse;
import com.thinkerscave.academics.dto.response.TimetableGridResponse;
import com.thinkerscave.academics.dto.response.TimetableVersionResponse;
import com.thinkerscave.academics.dto.response.TimetableWorkspaceResponse;

import java.time.LocalDate;
import java.util.List;

public interface TimetableWorkspaceService {

    TimetableWorkspaceResponse getWorkspace(Long yearId, LocalDate referenceDate);

    TimetableGridResponse getView(Long versionId, String view, Long academicGroupId, Long sectionId,
                                  Long staffId, Long resourceId, LocalDate weekStartDate);

    /** Create an empty editable draft so lessons can be entered by hand, without running the generator. */
    TimetableVersionResponse createManualDraft(Long yearId);

    /** Delete an editable draft and all of its entries, conflicts, runs and events. */
    void discardVersion(Long versionId);

    TimetableCellResponse addEntry(Long versionId, TimetableEntryRequest request);

    TimetableCellResponse updateEntry(Long entryId, TimetableEntryRequest request);

    void deleteEntry(Long entryId);

    List<TimetableConflictResponse> validateVersion(Long versionId);

    TimetableCellResponse setEntryLocked(Long entryId, boolean locked);
}
