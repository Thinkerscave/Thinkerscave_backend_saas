package com.thinkerscave.common.course.service;

import com.thinkerscave.common.course.dto.AcademicContainerDTO;
import com.thinkerscave.common.course.dto.StructureTemplateDTO;
import com.thinkerscave.common.course.dto.AcademicYearDTO;

import java.util.List;

/**
 * Service for managing academic structures (AcademicYear, AcademicContainer).
 */
public interface AcademicStructureService {

    // Academic Year
    AcademicYearDTO createAcademicYear(Long orgId, String yearCode, String startDate, String endDate);

    List<AcademicYearDTO> getAcademicYears(Long orgId);

    AcademicYearDTO getCurrentAcademicYear(Long orgId);

    void setCurrentAcademicYear(Long orgId, Long yearId);

    // Academic Container
    AcademicContainerDTO createContainer(AcademicContainerDTO dto);

    AcademicContainerDTO updateContainer(Long containerId, AcademicContainerDTO dto);

    AcademicContainerDTO getContainer(Long containerId);

    List<AcademicContainerDTO> getTopLevelContainers(Long orgId, Long yearId);

    List<AcademicContainerDTO> getChildContainers(Long parentId);

    void deleteContainer(Long containerId);

    // Institution Structure Generators
    void generateSchoolStructure(Long orgId, Long yearId);

    void generateCollegeStructure(Long orgId, Long yearId, Long courseId);

    void generateDynamicStructure(Long orgId, Long yearId, StructureTemplateDTO template);
}
