package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicClassRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicClassResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;

import java.util.List;

public interface AcademicStructureService {

    // Class operations
    AcademicClassResponse createClass(AcademicClassRequest request);

    AcademicClassResponse updateClass(Long classId, AcademicClassRequest request);

    AcademicClassResponse getClassById(Long classId);

    List<AcademicClassResponse> getClassesByYear(Long academicYearId);

    void deactivateClass(Long classId);

    // Section operations
    AcademicSectionResponse createSection(Long classId, AcademicSectionRequest request);

    AcademicSectionResponse updateSection(Long sectionId, AcademicSectionRequest request);

    AcademicSectionResponse getSectionById(Long sectionId);

    List<AcademicSectionResponse> getSectionsByClass(Long classId);

    void deactivateSection(Long sectionId);

    // Hierarchy
    List<AcademicStructureTreeResponse> getStructureTree(Long academicYearId);
}
