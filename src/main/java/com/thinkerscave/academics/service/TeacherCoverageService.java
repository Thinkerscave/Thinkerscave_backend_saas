package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.TeacherCoverageSaveRequest;
import com.thinkerscave.academics.dto.response.TeacherCandidateResponse;
import com.thinkerscave.academics.dto.response.TeacherCoverageBoardResponse;
import com.thinkerscave.academics.dto.response.TeacherCoveragePreviewResponse;

import java.util.List;

/** Teacher allocation at Academic Group level: group defaults, section overrides, and workload-aware saves. */
public interface TeacherCoverageService {

    TeacherCoverageBoardResponse getBoard(Long academicYearId, Long academicGroupId);

    List<TeacherCandidateResponse> getCandidates(Long academicYearId, Long academicGroupId,
                                                 Long mappingId, Long sectionId);

    TeacherCoveragePreviewResponse preview(Long academicYearId, TeacherCoverageSaveRequest request);

    TeacherCoverageBoardResponse save(Long academicYearId, TeacherCoverageSaveRequest request);
}
