package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicClassRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicClassResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;
import com.thinkerscave.academics.dto.response.ClassesSectionsDashboardResponse;
import com.thinkerscave.academics.enums.AcademicStage;

import java.util.List;

public interface AcademicStructureService {

    ClassesSectionsDashboardResponse getDashboard(Long academicYearId, String q, AcademicStage stage, Boolean active);

    AcademicClassResponse createClass(AcademicClassRequest request);

    AcademicClassResponse updateClass(Long classId, AcademicClassRequest request);

    AcademicClassResponse getClassById(Long classId);

    List<AcademicClassResponse> getClassesByYear(Long academicYearId);

    AcademicClassResponse deactivateClass(Long classId);

    AcademicClassResponse activateClass(Long classId);

    AcademicSectionResponse createSection(Long classId, AcademicSectionRequest request);

    AcademicSectionResponse updateSection(Long sectionId, AcademicSectionRequest request);

    AcademicSectionResponse getSectionById(Long sectionId);

    List<AcademicSectionResponse> getSectionsByClass(Long classId);

    AcademicSectionResponse deactivateSection(Long sectionId);

    AcademicSectionResponse activateSection(Long sectionId);

    List<AcademicStructureTreeResponse> getStructureTree(Long academicYearId);
}
