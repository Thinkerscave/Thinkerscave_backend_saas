package com.thinkerscave.common.course.controller;

import com.thinkerscave.common.course.dto.CourseRequestDTO;
import com.thinkerscave.common.course.dto.CourseResponseDTO;
import com.thinkerscave.common.course.dto.CourseSubjectMappingDTO;
import com.thinkerscave.common.course.service.CourseSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🎓 CourseController - The Academic Program Gateway
 * 
 * 🏛️ Business Purpose:
 * This controller provides the public REST interface for managing an
 * institution's academic programs. It allows administrators to register new
 * degrees, update course fees, and manage the lifecycle of institutional
 * offerings. It acts as the "Front Desk" for all curriculum header operations.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Academic Administrators**: Use these endpoints to set up the course
 * catalogue for a new academic year.
 * - **Integrators / Mobile Developers**: Consume these APIs to display the
 * school's course offerings on public-facing websites or student apps.
 * - **Finance Systems**: Synchronize with these endpoints to stay updated on
 * fee structures.
 * 
 * 🏗️ Design Intent:
 * Designed for **simplicity and discoverability**. Every endpoint is mapped to
 * a professional business operation and documented via Swagger/OpenAPI for
 * seamless developer onboarding.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/courses")
@Tag(name = "Course Management", description = "Professional APIs for defining and managing institutional degree programs and courses")
@RequiredArgsConstructor
public class CourseController {

    /**
     * Internal service handling the core course business logic.
     */
    private final CourseSubjectService courseService;

    /**
     * 🆕 createCourse
     * 
     * 🛠️ Purpose: Registers a new course/program into the tenant's catalogue.
     * 🏛️ Business Rationale: Required whenever a new department or degree is
     * launched.
     * 👥 Triggered by: Admin Portal (Setup Wizard).
     * 
     * @param dto Course metadata including duration, category, and fees.
     * @return ResponseEntity containing the persisted course details.
     */
    @PostMapping
    @Operation(summary = "Register a new academic program", description = "Creates a new Course entity linked to the organization. Generates a unique business code if not provided.")
    public ResponseEntity<CourseResponseDTO> createCourse(@RequestBody CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.createCourse(dto));
    }

    /**
     * 📝 updateCourse
     * 
     * 🛠️ Purpose: Modifies the administrative headers of an existing course.
     * 🏛️ Business Rationale: Used for yearly fee updates or curriculum name
     * adjustments.
     * 
     * @param courseId The unique identifier of the course to edit.
     * @param dto      Updated metadata.
     * @return ResponseEntity with the updated state.
     */
    @PutMapping("/{courseId}")
    @Operation(summary = "Update course administrative details", description = "Allows modification of name, description, fees, and eligibility. Use this for curriculum maintenance.")
    public ResponseEntity<CourseResponseDTO> updateCourse(@PathVariable Long courseId,
            @RequestBody CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, dto));
    }

    /**
     * 🔍 getCourse
     * 
     * 🛠️ Purpose: Fetches the full profile of a specific course.
     * 🏛️ Business Rationale: Used in course detail pages and enrollment forms.
     */
    @GetMapping("/{courseId}")
    @Operation(summary = "Fetch course profile", description = "Returns the full descriptive and administrative profile of a specific course.")
    public ResponseEntity<CourseResponseDTO> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourse(courseId));
    }

    /**
     * 🏢 getAllCoursesByOrg
     * 
     * 🛠️ Purpose: Provides a list of all courses offered by a specific
     * institution.
     * 🏛️ Business Rationale: Powers the course listing page in the student and
     * admin portals.
     * 
     * @param orgId The tenant organisation ID.
     * @return List of all courses belonging to the organisation.
     */
    @GetMapping("/org/{orgId}")
    @Operation(summary = "List all institutional offerings", description = "Fetches the complete course catalogue for a specific SaaS tenant (Organization).")
    public ResponseEntity<List<CourseResponseDTO>> getAllCoursesByOrg(@PathVariable Long orgId) {
        return ResponseEntity.ok(courseService.getAllCoursesByOrg(orgId));
    }

    /**
     * 🗑️ deleteCourse (Soft Delete)
     * 
     * 🛠️ Purpose: Retires a course from the active catalogue.
     * 🏛️ Business Rationale: Prevents new enrollments into legacy programs while
     * preserving data for alumni.
     */
    @DeleteMapping("/{courseId}")
    @Operation(summary = "Deactivate an academic program", description = "Performs a soft-delete (isActive=false) on the course. History is preserved but the course is hidden from active enrollment.")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Course-Subject Association Endpoints
    // -------------------------------------------------------------------------

    @PostMapping("/{courseId}/subjects")
    @Operation(summary = "Assign a subject to this course", description = "Links an existing subject to this course for a specific semester.")
    public ResponseEntity<Void> assignSubject(
            @PathVariable Long courseId,
            @RequestBody CourseSubjectMappingDTO mappingDTO) {

        // Ensure path variable integrity
        if (mappingDTO.getCourseId() == null) {
            mappingDTO.setCourseId(courseId);
        } else if (!mappingDTO.getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Course ID in path and body must match");
        }

        courseService.assignSubjectToCourse(mappingDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}/subjects/{subjectId}")
    @Operation(summary = "Remove a subject from this course", description = "Unlinks a subject from the course. Does not delete the subject entity.")
    public ResponseEntity<Void> removeSubject(
            @PathVariable Long courseId,
            @PathVariable Long subjectId) {
        courseService.removeSubjectFromCourse(courseId, subjectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{courseId}/subjects")
    @Operation(summary = "List all subjects in this course", description = "Returns the curriculum structure (subjects) for this course.")
    public ResponseEntity<List<CourseSubjectMappingDTO>> getSubjects(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getSubjectsByCourse(courseId));
    }
}
