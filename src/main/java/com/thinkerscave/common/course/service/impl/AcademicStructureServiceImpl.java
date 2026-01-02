package com.thinkerscave.common.course.service.impl;

import com.thinkerscave.common.course.domain.AcademicContainer;
import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.domain.Course;
import com.thinkerscave.common.course.dto.AcademicContainerDTO;
import com.thinkerscave.common.course.enums.ContainerType;
import com.thinkerscave.common.course.repository.AcademicContainerRepository;
import com.thinkerscave.common.course.repository.AcademicYearRepository;
import com.thinkerscave.common.course.repository.CourseRepository;
import com.thinkerscave.common.course.service.AcademicStructureService;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 🎓 AcademicStructureServiceImpl - The Institutional Blueprint Engine
 * 
 * 🏛️ Business Purpose:
 * This service is the "Architect" of the ThinkersCave platform. It defines how
 * an
 * institution is physically and logically organized. It manages the temporal
 * boundaries (Academic Years) and the structural hierarchy (Academic Containers
 * like Classes, Sections, Branches, and Batches).
 * 
 * 👥 User Roles & Stakeholders:
 * - **School Management / Owners**: Define the high-level structure (e.g., "We
 * are a
 * K-10 school" or "We are a Engineering College").
 * - **Admins**: Manage the year-on-year transition of students between these
 * containers.
 * - **Finance Module**: Uses these structures to apply fee templates (e.g.,
 * "Class 10
 * Fees" vs "B.Tech Year 1 Fees").
 * - **Attendance & Exam Modules**: Rely on these containers as the base unit
 * for
 * grouping students for operations.
 * 
 * 🔄 Academic Flow Position:
 * This is the **Infrastructure Phase**. Before a single student can be admitted
 * or a subject can be taught, the school must have an active Academic Year
 * and a valid Class/Section structure.
 * 
 * 🏗️ Design Intent:
 * Built using a **Composite Pattern** (Parent-Child containers). This allows
 * ThinkersCave to support a diverse range of customers—from a small tutoring
 * center (1 level) to a massive multi-campus university (multi-level).
 * 
 * 🚀 Future Extensibility:
 * - Automated "Promotion Engine" to move students across containers at
 * year-end.
 * - Dynamic capacity management and classroom assignment.
 * - Cross-year structural templates for easy scaling of new branches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicStructureServiceImpl implements AcademicStructureService {

    /**
     * Repository layer for Academic Years.
     * Manages the "Timeline" of the institution.
     */
    private final AcademicYearRepository academicYearRepository;

    /**
     * Repository layer for Academic Containers.
     * The master repository for the hierarchical structure (Classes/Sections/etc).
     */
    private final AcademicContainerRepository academicContainerRepository;

    /**
     * Repository layer for Organisations.
     * Ensures all structures are logically partitioned by tenant.
     */
    private final OrganizationRepository organizationRepository;

    /**
     * Repository layer for Courses.
     * Used when a container is specific to a degree program (e.g., "CS Branch").
     */
    private final CourseRepository courseRepository;

    /**
     * 📅 createAcademicYear
     * 
     * 🛠️ Purpose: Defines a new calendar cycle for the institution.
     * ⏰ When it is called: Typically once a year during the planning phase.
     * 👤 Triggered by: Administrator (System Settings).
     * 
     * @param orgId     The tenant's ID.
     * @param yearCode  Human-readable code (e.g., "2024-25").
     * @param startDate Date when the semester/session begins.
     * @param endDate   Date when the session ends.
     * @return AcademicYear The persisted record.
     */
    @Override
    @Transactional
    public AcademicYear createAcademicYear(Long orgId, String yearCode, String startDate, String endDate) {
        Organisation org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found with ID: " + orgId));

        AcademicYear academicYear = new AcademicYear();

        // Business Rule: The code and name are usually identical for simplicity in
        // dropdowns.
        academicYear.setYearCode(yearCode);
        academicYear.setYearName(yearCode);

        // Logical check: Date strings are parsed to ISO-8601 local dates.
        academicYear.setStartDate(LocalDate.parse(startDate));
        academicYear.setEndDate(LocalDate.parse(endDate));
        academicYear.setOrganization(org);

        // New years are initialized as non-current to prevent accidental active
        // switches.
        academicYear.setIsCurrent(false);
        academicYear.setIsActive(true);

        log.info("Creating new Academic Year: {} for Org: {}", yearCode, org.getOrgName());
        return academicYearRepository.save(academicYear);
    }

    /**
     * 🔍 getAcademicYears
     * 
     * 🛠️ Purpose: Lists all historical and future calendar cycles.
     */
    @Override
    public List<AcademicYear> getAcademicYears(Long orgId) {
        Organisation org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found with ID: " + orgId));
        return academicYearRepository.findByOrganization(org);
    }

    /**
     * 📍 getCurrentAcademicYear
     * 
     * 🛠️ Purpose: Identifies the "Active" session for the tenant.
     * 🏛️ Business Rationale: Most system modules (Syllabus, Attendance) default
     * to the 'Current' year data.
     */
    @Override
    public AcademicYear getCurrentAcademicYear(Long orgId) {
        Organisation org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found with ID: " + orgId));
        return academicYearRepository.findByOrganizationAndIsCurrentTrue(org)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No current academic year found for organisation ID: " + orgId));
    }

    /**
     * 🔄 setCurrentAcademicYear
     * 
     * 🛠️ Purpose: Switches the active session institutional-wide.
     * ⚠️ Side Effects: Updates ALL year records for the org to ensure only ONE is
     * current.
     */
    @Override
    @Transactional
    public void setCurrentAcademicYear(Long orgId, Long yearId) {
        Organisation org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found with ID: " + orgId));

        // Logic Rationale: This is a state-machine transition. We fetch all years
        // and perform a bulk toggle to maintain the "Only One Current" invariant.
        List<AcademicYear> allYears = academicYearRepository.findByOrganization(org);
        for (AcademicYear year : allYears) {
            year.setIsCurrent(year.getAcademicYearId().equals(yearId));
        }
        academicYearRepository.saveAll(allYears);
        log.info("Organisation {} has switched active year to ID: {}", org.getOrgName(), yearId);
    }

    /**
     * 🆕 createContainer
     * 
     * 🛠️ Purpose: Adds a new node to the institutional hierarchy.
     * 🏛️ Logic Flow:
     * 1. Validate parent-child relationship.
     * 2. Inherit/Assign Course context if applicable.
     * 3. Register with unique business code.
     * 
     * @param dto Container details including type (CLASS, SECTION, etc.) and
     *            hierarchy level.
     * @return AcademicContainerDTO
     */
    @Override
    @Transactional
    public AcademicContainerDTO createContainer(AcademicContainerDTO dto) {
        Organisation org = organizationRepository.findById(dto.getOrganisationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
        AcademicYear year = academicYearRepository.findById(dto.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic Year not found"));

        AcademicContainer container = new AcademicContainer();
        container.setContainerType(dto.getContainerType());

        // Code Generation: Use the type-specific prefix (e.g., CLASS-XXXXX).
        container.setContainerCode(
                dto.getContainerCode() != null ? dto.getContainerCode() : generateCode(dto.getContainerType()));

        container.setContainerName(dto.getContainerName());
        container.setOrganization(org);
        container.setAcademicYear(year);
        container.setLevel(dto.getLevel());
        container.setCapacity(dto.getCapacity());

        // Strength Tracking: Initialized to zero. Incremented by the Student Admission
        // service.
        container.setCurrentStrength(0);
        container.setIsActive(true);

        // Optional Association: Some containers (like Branches) are linked to specific
        // Courses.
        if (dto.getCourseId() != null) {
            Course course = courseRepository.findById(dto.getCourseId()).orElse(null);
            container.setCourse(course);
        }

        // Parent Association: Creates the tree structure.
        // Example: 'Section A' (Child) -> 'Class 10' (Parent).
        if (dto.getParentContainerId() != null) {
            AcademicContainer parent = academicContainerRepository.findById(dto.getParentContainerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent container not found"));
            container.setParentContainer(parent);
        }

        AcademicContainer saved = academicContainerRepository.save(container);
        return mapToDTO(saved);
    }

    /**
     * 📝 updateContainer
     * 
     * 🛠️ Purpose: Updates administrative details (e.g., increasing capacity).
     */
    @Override
    @Transactional
    public AcademicContainerDTO updateContainer(Long containerId, AcademicContainerDTO dto) {
        AcademicContainer container = academicContainerRepository.findById(containerId)
                .orElseThrow(() -> new ResourceNotFoundException("Container not found"));

        container.setContainerName(dto.getContainerName());
        container.setCapacity(dto.getCapacity());

        AcademicContainer saved = academicContainerRepository.save(container);
        return mapToDTO(saved);
    }

    @Override
    public AcademicContainerDTO getContainer(Long containerId) {
        AcademicContainer container = academicContainerRepository.findById(containerId)
                .orElseThrow(() -> new ResourceNotFoundException("Container not found"));
        return mapToDTO(container);
    }

    /**
     * 🔝 getTopLevelContainers
     * 
     * 🛠️ Purpose: Finds the "Roots" of the institution (e.g., all Grades/Classes).
     */
    @Override
    public List<AcademicContainerDTO> getTopLevelContainers(Long orgId, Long yearId) {
        Organisation org = organizationRepository.findById(orgId).orElseThrow();
        AcademicYear year = academicYearRepository.findById(yearId).orElseThrow();
        return academicContainerRepository.findByOrganizationAndAcademicYearAndParentContainerIsNull(org, year)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * 👨‍👩‍👧 getChildContainers
     * 
     * 🛠️ Purpose: Drills down into a structure (e.g., finding Sections for a
     * Class).
     */
    @Override
    public List<AcademicContainerDTO> getChildContainers(Long parentId) {
        AcademicContainer parent = academicContainerRepository.findById(parentId).orElseThrow();
        return academicContainerRepository.findByParentContainer(parent)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * 🗑️ deleteContainer
     * 
     * ⚠️ Caution: This is a hard-delete. Should only be allowed if no
     * students are assigned.
     */
    @Override
    @Transactional
    public void deleteContainer(Long containerId) {
        academicContainerRepository.deleteById(containerId);
    }

    /**
     * 🏭 generateSchoolStructure
     * 
     * 🛠️ Purpose: Automates onboarding for K-12 schools.
     * 🏛️ Business Rationale: Manually creating 10-15 classes and 30-40 sections
     * is tedious. This utility provides a "Quick-Start" for school admins.
     */
    @Override
    @Transactional
    public void generateSchoolStructure(Long orgId, Long yearId) {
        // Business Logic: Loops through standard grades for a primary/secondary school.
        for (int i = 1; i <= 10; i++) {
            AcademicContainerDTO classDto = AcademicContainerDTO.builder()
                    .containerType(ContainerType.CLASS)
                    .containerName("Class " + i)
                    .organisationId(orgId)
                    .academicYearId(yearId)
                    .level(1)
                    .build();
            AcademicContainerDTO savedClass = createContainer(classDto);

            // Defaults to creating at least one section (A) for every class.
            AcademicContainerDTO sectionDto = AcademicContainerDTO.builder()
                    .containerType(ContainerType.SECTION)
                    .containerName("Section A")
                    .organisationId(orgId)
                    .academicYearId(yearId)
                    .parentContainerId(savedClass.getContainerId())
                    .level(2)
                    .build();
            createContainer(sectionDto);
        }
    }

    /**
     * 🎓 generateCollegeStructure
     * 
     * 🛠️ Purpose: Automates onboarding for Higher Education.
     * 🏛️ Business Rationale: Higher-ed uses a 'Branch -> Year' structure.
     * This utility adapts the generic 'Academic Container' model to the
     * College persona.
     */
    @Override
    @Transactional
    public void generateCollegeStructure(Long orgId, Long yearId, Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow();

        // 1. Create the Branch (e.g., Computer Science Engineering)
        AcademicContainerDTO branchDto = AcademicContainerDTO.builder()
                .containerType(ContainerType.BRANCH)
                .containerName(course.getCourseName())
                .organisationId(orgId)
                .academicYearId(yearId)
                .courseId(courseId)
                .level(1)
                .build();
        AcademicContainerDTO savedBranch = createContainer(branchDto);

        // 2. Create nested Years based on course duration.
        for (int i = 1; i <= course.getDurationYears(); i++) {
            AcademicContainerDTO yearDto = AcademicContainerDTO.builder()
                    .containerType(ContainerType.YEAR)
                    .containerName("Year " + i)
                    .organisationId(orgId)
                    .academicYearId(yearId)
                    .parentContainerId(savedBranch.getContainerId())
                    .level(2)
                    .build();
            createContainer(yearDto);
        }
    }

    /**
     * Maps Entity to DTO while preserving the business-level hierarchy identifiers.
     */
    private AcademicContainerDTO mapToDTO(AcademicContainer container) {
        return AcademicContainerDTO.builder()
                .containerId(container.getContainerId())
                .containerType(container.getContainerType())
                .containerCode(container.getContainerCode())
                .containerName(container.getContainerName())
                .organisationId(container.getOrganization().getOrgId())
                .academicYearId(container.getAcademicYear().getAcademicYearId())
                .courseId(container.getCourse() != null ? container.getCourse().getCourseId() : null)
                .parentContainerId(
                        container.getParentContainer() != null ? container.getParentContainer().getContainerId()
                                : null)
                .level(container.getLevel())
                .capacity(container.getCapacity())
                .currentStrength(container.getCurrentStrength())
                .build();
    }

    /**
     * Code Utility: Generates stable internal identifiers with a type prefix.
     */
    private String generateCode(ContainerType type) {
        return type.name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
