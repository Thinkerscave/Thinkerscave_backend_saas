package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicClassRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicClassResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.enums.AcademicStage;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.service.AcademicStructureService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicStructureServiceImpl implements AcademicStructureService {

    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;

    // ---- Class operations ----

    @Override
    @Transactional
    public AcademicClassResponse createClass(AcademicClassRequest request) {
        AcademicYear year = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + request.getAcademicYearId()));
        if (classRepository.existsByAcademicYear_AcademicYearIdAndClassCode(request.getAcademicYearId(), request.getClassCode())) {
            throw new AlreadyExistsException("Class code '" + request.getClassCode() + "' already exists for this academic year");
        }
        if (classRepository.existsByAcademicYear_AcademicYearIdAndClassNameIgnoreCase(request.getAcademicYearId(), request.getClassName())) {
            throw new AlreadyExistsException("Class name '" + request.getClassName() + "' already exists for this academic year");
        }
        AcademicClass cls = new AcademicClass();
        cls.setAcademicYear(year);
        mapClassRequest(request, cls);
        cls.setActive(true);
        return toClassResponse(classRepository.save(cls));
    }

    @Override
    @Transactional
    public AcademicClassResponse updateClass(Long classId, AcademicClassRequest request) {
        AcademicClass cls = findClassById(classId);
        if (classRepository.existsByAcademicYear_AcademicYearIdAndClassCodeAndClassIdNot(
                cls.getAcademicYear().getAcademicYearId(), request.getClassCode(), classId)) {
            throw new AlreadyExistsException("Class code '" + request.getClassCode() + "' already exists for this academic year");
        }
        if (classRepository.existsByAcademicYear_AcademicYearIdAndClassNameIgnoreCaseAndClassIdNot(
                cls.getAcademicYear().getAcademicYearId(), request.getClassName(), classId)) {
            throw new AlreadyExistsException("Class name '" + request.getClassName() + "' already exists for this academic year");
        }
        mapClassRequest(request, cls);
        return toClassResponse(classRepository.save(cls));
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicClassResponse getClassById(Long classId) {
        return toClassResponse(findClassById(classId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicClassResponse> getClassesByYear(Long academicYearId) {
        return classRepository.findWithYearByAcademicYearIdOrderByDisplayOrderAsc(academicYearId)
                .stream().map(this::toClassResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateClass(Long classId) {
        AcademicClass cls = findClassById(classId);
        if (sectionRepository.existsByAcademicClass_ClassId(classId)) {
            throw new BadRequestException("Cannot deactivate class with existing sections. Deactivate sections first.");
        }
        cls.setActive(false);
        classRepository.save(cls);
    }

    // ---- Section operations ----

    @Override
    @Transactional
    public AcademicSectionResponse createSection(Long classId, AcademicSectionRequest request) {
        AcademicClass cls = findClassById(classId);
        if (sectionRepository.existsByAcademicClass_ClassIdAndSectionName(classId, request.getSectionName())) {
            throw new AlreadyExistsException("Section '" + request.getSectionName() + "' already exists in this class");
        }
        AcademicSection section = new AcademicSection();
        section.setAcademicClass(cls);
        mapSectionRequest(request, section);
        section.setActive(true);
        return toSectionResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public AcademicSectionResponse updateSection(Long sectionId, AcademicSectionRequest request) {
        AcademicSection section = findSectionById(sectionId);
        if (sectionRepository.existsByAcademicClass_ClassIdAndSectionNameAndSectionIdNot(
                section.getAcademicClass().getClassId(), request.getSectionName(), sectionId)) {
            throw new AlreadyExistsException("Section name '" + request.getSectionName() + "' already exists in this class");
        }
        mapSectionRequest(request, section);
        return toSectionResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicSectionResponse getSectionById(Long sectionId) {
        return toSectionResponse(findSectionById(sectionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicSectionResponse> getSectionsByClass(Long classId) {
        return sectionRepository.findWithClassByAcademicClass_ClassIdOrderBySectionNameAsc(classId)
                .stream().map(this::toSectionResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateSection(Long sectionId) {
        AcademicSection section = findSectionById(sectionId);
        section.setActive(false);
        sectionRepository.save(section);
    }

    // ---- Hierarchy ----

    @Override
    @Transactional(readOnly = true)
    public List<AcademicStructureTreeResponse> getStructureTree(Long academicYearId) {
        List<AcademicClass> classes = classRepository.findWithYearByAcademicYearIdAndActiveOrderByDisplayOrderAsc(academicYearId, true);
        Map<AcademicStage, List<AcademicClass>> byStage = classes.stream()
                .collect(Collectors.groupingBy(AcademicClass::getAcademicStage));

        return Arrays.stream(AcademicStage.values())
                .filter(byStage::containsKey)
                .map(stage -> {
                    List<AcademicStructureTreeResponse.ClassNode> classNodes = byStage.get(stage).stream()
                            .map(cls -> {
                                List<AcademicSection> sections = sectionRepository.findWithClassByAcademicClass_ClassIdAndActiveOrderBySectionNameAsc(cls.getClassId(), true);
                                List<AcademicStructureTreeResponse.SectionNode> sectionNodes = sections.stream()
                                        .map(s -> AcademicStructureTreeResponse.SectionNode.builder()
                                                .sectionId(s.getSectionId())
                                                .sectionName(s.getSectionName())
                                                .capacity(s.getCapacity())
                                                .build())
                                        .collect(Collectors.toList());
                                return AcademicStructureTreeResponse.ClassNode.builder()
                                        .classId(cls.getClassId())
                                        .className(cls.getClassName())
                                        .classCode(cls.getClassCode())
                                        .displayOrder(cls.getDisplayOrder())
                                        .sections(sectionNodes)
                                        .build();
                            })
                            .collect(Collectors.toList());
                    return AcademicStructureTreeResponse.builder()
                            .stage(stage.name())
                            .classes(classNodes)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ---- helpers ----

    private AcademicClass findClassById(Long classId) {
        return classRepository.findByIdWithYear(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic class not found with id: " + classId));
    }

    private AcademicSection findSectionById(Long sectionId) {
        return sectionRepository.findByIdWithClass(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));
    }

    private void mapClassRequest(AcademicClassRequest request, AcademicClass cls) {
        cls.setClassCode(request.getClassCode());
        cls.setClassName(request.getClassName());
        cls.setAcademicStage(AcademicStage.valueOf(request.getAcademicStage()));
        cls.setDisplayOrder(request.getDisplayOrder());
        cls.setRemarks(request.getRemarks());
    }

    private void mapSectionRequest(AcademicSectionRequest request, AcademicSection section) {
        section.setSectionName(request.getSectionName());
        section.setCapacity(request.getCapacity());
        section.setRemarks(request.getRemarks());
    }

    private AcademicClassResponse toClassResponse(AcademicClass cls) {
        return AcademicClassResponse.builder()
                .classId(cls.getClassId())
                .academicYearId(cls.getAcademicYear().getAcademicYearId())
                .yearCode(cls.getAcademicYear().getYearCode())
                .classCode(cls.getClassCode())
                .className(cls.getClassName())
                .academicStage(cls.getAcademicStage().name())
                .displayOrder(cls.getDisplayOrder())
                .active(cls.getActive())
                .remarks(cls.getRemarks())
                .createdOn(cls.getCreatedOn())
                .build();
    }

    private AcademicSectionResponse toSectionResponse(AcademicSection section) {
        return AcademicSectionResponse.builder()
                .sectionId(section.getSectionId())
                .classId(section.getAcademicClass().getClassId())
                .className(section.getAcademicClass().getClassName())
                .sectionName(section.getSectionName())
                .capacity(section.getCapacity())
                .active(section.getActive())
                .remarks(section.getRemarks())
                .createdOn(section.getCreatedOn())
                .build();
    }
}
