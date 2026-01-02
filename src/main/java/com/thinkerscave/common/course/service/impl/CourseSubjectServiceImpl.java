package com.thinkerscave.common.course.service.impl;

import com.thinkerscave.common.course.domain.Course;
import com.thinkerscave.common.course.domain.Subject;
import com.thinkerscave.common.course.dto.CourseRequestDTO;
import com.thinkerscave.common.course.dto.CourseResponseDTO;
import com.thinkerscave.common.course.dto.SubjectRequestDTO;
import com.thinkerscave.common.course.dto.SubjectResponseDTO;
import com.thinkerscave.common.course.repository.CourseRepository;
import com.thinkerscave.common.course.repository.SubjectRepository;
import com.thinkerscave.common.course.service.CourseSubjectService;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 🎓 CourseSubjectServiceImpl - The Academic Catalogue Management Engine
 * 
 * 🏛️ Business Purpose:
 * This service is responsible for defining the structural foundations of an
 * educational
 * institution within the ThinkersCave SaaS platform. It manages "Courses" (the
 * high-level
 * degrees or certifications like 'B.Sc Computer Science') and "Subjects" (the
 * individual
 * modules of study like 'Data Structures').
 * 
 * 👥 User Roles & Stakeholders:
 * - **Registrar / Academic Admins**: Primary users who define the course
 * catalogue
 * and fee structures during institutional setup.
 * - **Academic Heads (HODs)**: Define and update the technical details of
 * subjects
 * including credits and contact hours.
 * - **Admissions Team**: Relies on Course duration and eligibility data stored
 * here
 * to process student applications.
 * - **System Modules**: Enrollment and Finance modules consume the data from
 * this
 * service to calculate fees and generate academic transcripts.
 * 
 * 🔄 Academic Flow Position:
 * This is the **Primary Configuration Phase**. A Syllabus cannot be created
 * without a Subject, and a Subject usually exists within the context of a
 * Course/Organisation.
 * 
 * 🏗️ Design Intent:
 * Built with a "Multi-Tenant First" approach. Every course and subject is
 * strictly
 * scoped to an `Organisation` to ensure data isolation between different
 * schools or
 * universities sharing the platform.
 * 
 * 🚀 Future Extensibility:
 * - Integration with global education standards (e.g., ECTS credits).
 * - Support for pre-requisite subject mapping (Subject A must be taken before
 * Subject B).
 * - Automated fee calculation logic based on course credits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSubjectServiceImpl implements CourseSubjectService {

    /**
     * Repository layer for Courses.
     * Stores high-level certification data, duration, and eligibility.
     */
    private final CourseRepository courseRepository;

    /**
     * Repository layer for Subjects.
     * Manages modular units of study, including credits and hourly requirements.
     */
    private final SubjectRepository subjectRepository;

    /**
     * Repository layer for Organisations.
     * Essential for verifying that every course/subject belongs to a valid tenant.
     */
    private final OrganizationRepository organizationRepository;

    /**
     * 🆕 createCourse
     * 
     * 🛠️ Purpose: Registers a new academic course/degree program.
     * ⏰ When it is called: During the initial onboarding of a school or when
     * a new department is launched.
     * 👤 Triggered by: Admin Portal (Course Management).
     * 
     * @param dto Object containing course metadata, duration, and fees.
     * @return CourseResponseDTO The persisted course record with generated IDs.
     * 
     *         ⚠️ Side Effects: DB insertion into the 'courses' table.
     */
    @Override
    @Transactional
    public CourseResponseDTO createCourse(CourseRequestDTO dto) {
        // Multi-tenant validation: Ensure the hosting organisation exists.
        Organisation org = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));

        Course course = new Course();

        // Logical check: Generate a business-friendly code if not provided manually.
        course.setCourseCode(dto.getCourseCode() != null ? dto.getCourseCode() : generateCode("CRS"));

        course.setCourseName(dto.getCourseName());
        course.setDescription(dto.getDescription());
        course.setCategory(dto.getCategory());
        course.setDurationYears(dto.getDurationYears());
        course.setTotalSemesters(dto.getTotalSemesters());
        course.setEligibilityCriteria(dto.getEligibilityCriteria());
        course.setFees(dto.getFees());
        course.setOrganization(org);

        // Availability Control: New courses are active by default for immediate
        // enrollment.
        course.setIsActive(true);

        Course saved = courseRepository.save(course);
        log.info("New Course created: {} (ID: {}) for Org: {}", saved.getCourseName(), saved.getCourseId(),
                org.getOrgName());
        return mapToCourseDTO(saved);
    }

    /**
     * 📝 updateCourse
     * 
     * 🛠️ Purpose: Updates the details of an existing course (e.g., fee changes or
     * name updates).
     * ⏰ When it is called: When curriculum headers or administrative details
     * change.
     * 
     * @param courseId The ID of the course to edit.
     * @param dto      New metadata.
     * @return CourseResponseDTO The updated state.
     */
    @Override
    @Transactional
    public CourseResponseDTO updateCourse(Long courseId, CourseRequestDTO dto) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Logic rationale: We update only mutable fields.
        // Organisation and CourseCode are usually immutable after creation to
        // maintain audit trails and reference integrity.
        course.setCourseName(dto.getCourseName());
        course.setDescription(dto.getDescription());
        course.setCategory(dto.getCategory());
        course.setDurationYears(dto.getDurationYears());
        course.setTotalSemesters(dto.getTotalSemesters());
        course.setEligibilityCriteria(dto.getEligibilityCriteria());
        course.setFees(dto.getFees());

        Course saved = courseRepository.save(course);
        return mapToCourseDTO(saved);
    }

    /**
     * 🔍 getCourse
     * 
     * 🛠️ Purpose: Fetches header information for a specific course.
     * 
     * @param courseId The ID of the course to retrieve.
     * @return CourseResponseDTO
     */
    @Override
    public CourseResponseDTO getCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return mapToCourseDTO(course);
    }

    /**
     * 🏢 getAllCoursesByOrg
     * 
     * 🛠️ Purpose: Lists all academic offerings for a specific tenant.
     * 👤 Who triggers it: Prospective parents/students viewing the school's public
     * catalogue.
     * 
     * @param orgId The tenant's unique ID.
     * @return List of courses belonging to the organisation.
     */
    @Override
    public List<CourseResponseDTO> getAllCoursesByOrg(Long orgId) {
        Organisation org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
        return courseRepository.findByOrganization(org)
                .stream().map(this::mapToCourseDTO).collect(Collectors.toList());
    }

    /**
     * 🗑️ deleteCourse
     * 
     * 🛠️ Purpose: Retires a course from active selection.
     * 
     * 🔒 Strategy: Soft Delete.
     * Why: We cannot hard-delete courses once students have been enrolled, as
     * it would break the historical academic record of the alumni.
     */
    @Override
    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        course.setIsActive(false);
        courseRepository.save(course);
        log.warn("Course ID: {} has been deactivated (soft-deleted).", courseId);
    }

    /**
     * 🆕 createSubject
     * 
     * 🛠️ Purpose: Defines a new subject/module in the system.
     * ⏰ When it is called: When a new subject is added to the curriculum.
     * 
     * @param dto Details about credits, theory vs lab hours, and category.
     * @return SubjectResponseDTO
     */
    @Override
    @Transactional
    public SubjectResponseDTO createSubject(SubjectRequestDTO dto) {
        Organisation org = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));

        Subject subject = new Subject();

        // Business Rule: Use standard codes (e.g., CS101) for academic tracking.
        subject.setSubjectCode(dto.getSubjectCode() != null ? dto.getSubjectCode() : generateCode("SUB"));

        subject.setSubjectName(dto.getSubjectName());
        subject.setDescription(dto.getDescription());
        subject.setCategory(dto.getCategory());
        subject.setCredits(dto.getCredits());
        subject.setTheoryHours(dto.getTheoryHours());
        subject.setLabHours(dto.getLabHours());
        subject.setPracticalHours(dto.getPracticalHours());
        subject.setOrganization(org);
        subject.setIsActive(true);

        Subject saved = subjectRepository.save(subject);
        return mapToSubjectDTO(saved);
    }

    /**
     * 📝 updateSubject
     * 
     * 🛠️ Purpose: Updates subject parameters like credits or description.
     */
    @Override
    @Transactional
    public SubjectResponseDTO updateSubject(Long subjectId, SubjectRequestDTO dto) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        subject.setSubjectName(dto.getSubjectName());
        subject.setDescription(dto.getDescription());
        subject.setCategory(dto.getCategory());
        subject.setCredits(dto.getCredits());
        subject.setTheoryHours(dto.getTheoryHours());
        subject.setLabHours(dto.getLabHours());
        subject.setPracticalHours(dto.getPracticalHours());

        Subject saved = subjectRepository.save(subject);
        return mapToSubjectDTO(saved);
    }

    @Override
    public SubjectResponseDTO getSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        return mapToSubjectDTO(subject);
    }

    @Override
    public List<SubjectResponseDTO> getAllSubjectsByOrg(Long orgId) {
        Organisation org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
        return subjectRepository.findByOrganization(org)
                .stream().map(this::mapToSubjectDTO).collect(Collectors.toList());
    }

    /**
     * 🗑️ deleteSubject
     * 
     * 🔒 Strategy: Soft Delete.
     * Why: Preserves data integrity for old syllabi that might still refer
     * to this subject code.
     */
    @Override
    @Transactional
    public void deleteSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        subject.setIsActive(false);
        subjectRepository.save(subject);
    }

    /**
     * Helper to convert Course Entity to DTO.
     */
    private CourseResponseDTO mapToCourseDTO(Course course) {
        return CourseResponseDTO.builder()
                .courseId(course.getCourseId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .category(course.getCategory())
                .durationYears(course.getDurationYears())
                .totalSemesters(course.getTotalSemesters())
                .eligibilityCriteria(course.getEligibilityCriteria())
                .fees(course.getFees())
                .isActive(course.getIsActive())
                .build();
    }

    /**
     * Helper to convert Subject Entity to DTO.
     */
    private SubjectResponseDTO mapToSubjectDTO(Subject subject) {
        return SubjectResponseDTO.builder()
                .subjectId(subject.getSubjectId())
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .description(subject.getDescription())
                .category(subject.getCategory())
                .credits(subject.getCredits())
                .theoryHours(subject.getTheoryHours())
                .labHours(subject.getLabHours())
                .practicalHours(subject.getPracticalHours())
                .isActive(subject.getIsActive())
                .build();
    }

    /**
     * Code Utility: Generates unique internal identifiers.
     */
    private String generateCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
