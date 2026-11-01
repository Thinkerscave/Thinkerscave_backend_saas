package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicGroupRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicGroupResponse;
import com.thinkerscave.academics.dto.response.AcademicGroupSummaryResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;
import com.thinkerscave.academics.dto.response.AcademicGroupsDashboardResponse;
import com.thinkerscave.academics.enums.AcademicStage;

import java.util.List;

public interface AcademicStructureService {

    AcademicGroupsDashboardResponse getDashboard(Long academicYearId, String q, AcademicStage stage, Boolean active);

    AcademicGroupResponse createAcademicGroup(AcademicGroupRequest request);

    AcademicGroupResponse updateAcademicGroup(Long academicGroupId, AcademicGroupRequest request);

    AcademicGroupResponse getAcademicGroupById(Long academicGroupId);

    List<AcademicGroupResponse> getAcademicGroupsByYear(Long academicYearId);

    AcademicGroupResponse deactivateAcademicGroup(Long academicGroupId);

    AcademicGroupResponse activateAcademicGroup(Long academicGroupId);

    AcademicSectionResponse createSection(Long academicGroupId, AcademicSectionRequest request);

    AcademicSectionResponse updateSection(Long sectionId, AcademicSectionRequest request);

    AcademicSectionResponse getSectionById(Long sectionId);

    List<AcademicSectionResponse> getSectionsByAcademicGroup(Long academicGroupId);

    AcademicSectionResponse deactivateSection(Long sectionId);

    AcademicSectionResponse activateSection(Long sectionId);

    List<AcademicStructureTreeResponse> getStructureTree(Long academicYearId);

    AcademicGroupSummaryResponse getGroupSummary(Long academicGroupId);
}
