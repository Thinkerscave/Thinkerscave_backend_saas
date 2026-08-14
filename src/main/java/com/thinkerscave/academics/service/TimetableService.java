package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicResourceRequest;
import com.thinkerscave.academics.dto.request.TimetableConfigurationRequest;
import com.thinkerscave.academics.dto.request.TimetableGenerationStartRequest;
import com.thinkerscave.academics.dto.response.*;

import java.util.List;

public interface TimetableService {

    TimetableDashboardResponse getDashboard(Long yearId);

    TimetableConfigurationResponse getConfiguration(Long yearId);

    TimetableConfigurationResponse upsertConfiguration(Long yearId, TimetableConfigurationRequest request);

    List<AcademicResourceResponse> listResources();

    AcademicResourceResponse createResource(AcademicResourceRequest request);

    AcademicResourceResponse updateResource(Long id, AcademicResourceRequest request);

    AcademicResourceResponse deactivateResource(Long id);

    TimetableReadinessResponse evaluateReadiness(Long yearId);

    TimetableGenerationAcceptedResponse startGeneration(Long yearId, TimetableGenerationStartRequest request);

    TimetableGenerationProgressResponse getGenerationProgress(Long generationId);

    void cancelGeneration(Long generationId);

    /** @deprecated Use {@link #startGeneration} instead. Kept for backward compatibility. */
    @Deprecated
    TimetableGenerateResultResponse generate(Long yearId);

    TimetableGridResponse getGrid(Long versionId, String view, Long sectionId, Long staffId, Long resourceId);

    List<TimetableConflictResponse> getConflicts(Long versionId);

    TimetableConflictResponse resolveConflict(Long conflictId);

    TimetableConflictResponse ignoreConflict(Long conflictId);

    List<TimetableVersionResponse> listVersions(Long yearId);

    TimetableVersionResponse submitVersion(Long versionId);

    TimetableVersionResponse approveVersion(Long versionId);

    TimetableVersionResponse rejectVersion(Long versionId);

    TimetableVersionResponse publishVersion(Long versionId);
}
