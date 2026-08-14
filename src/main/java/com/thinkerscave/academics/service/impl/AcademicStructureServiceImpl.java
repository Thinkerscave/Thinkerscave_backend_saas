package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicClassRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicClassResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;
import com.thinkerscave.academics.service.AcademicStructureService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AcademicStructureServiceImpl implements AcademicStructureService {

    private static final String MSG = "Academics structure API rebuild in progress";

    @Override
    public AcademicClassResponse createClass(AcademicClassRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicClassResponse updateClass(Long classId, AcademicClassRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicClassResponse getClassById(Long classId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<AcademicClassResponse> getClassesByYear(Long academicYearId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void deactivateClass(Long classId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicSectionResponse createSection(Long classId, AcademicSectionRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicSectionResponse updateSection(Long sectionId, AcademicSectionRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicSectionResponse getSectionById(Long sectionId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<AcademicSectionResponse> getSectionsByClass(Long classId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void deactivateSection(Long sectionId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<AcademicStructureTreeResponse> getStructureTree(Long academicYearId) {
        throw new UnsupportedOperationException(MSG);
    }
}
