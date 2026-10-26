package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.TimetableEntryRequest;
import com.thinkerscave.academics.dto.response.TimetableCellResponse;
import com.thinkerscave.academics.dto.response.TimetableConflictResponse;
import com.thinkerscave.academics.dto.response.TimetableGridResponse;
import com.thinkerscave.academics.dto.response.TimetableWorkspaceResponse;

import java.time.LocalDate;
import java.util.List;

public interface TimetableWorkspaceService {

    TimetableWorkspaceResponse getWorkspace(Long yearId, LocalDate referenceDate);

    TimetableGridResponse getView(Long versionId, String view, Long academicGroupId, Long sectionId,
                                  Long staffId, Long resourceId, LocalDate weekStartDate);

    TimetableCellResponse addEntry(Long versionId, TimetableEntryRequest request);

    TimetableCellResponse updateEntry(Long entryId, TimetableEntryRequest request);

    void deleteEntry(Long entryId);

    List<TimetableConflictResponse> validateVersion(Long versionId);

    TimetableCellResponse setEntryLocked(Long entryId, boolean locked);
}
