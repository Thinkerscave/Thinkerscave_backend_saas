package com.thinkerscave.common.course.controller;

import com.thinkerscave.common.course.dto.SubjectRequestDTO;
import com.thinkerscave.common.course.dto.SubjectResponseDTO;
import com.thinkerscave.common.course.service.CourseSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 📚 SubjectController - The Academic Module Interface
 * 
 * 🏛️ Business Purpose:
 * This controller manages the "Academic Units" or Subjects of an institution.
 * While Courses represent whole programs, Subjects represent the individual
 * modules of knowledge. This interface allows Academic Heads to define credits,
 * workload hours, and evaluation standards for every subject in the curriculum.
 * 
 * 👥 User Roles & Stakeholders:
 * - **HODs / Subject Matter Experts**: Create and refine the subject
 * definitions.
 * - **Timetable Managers**: Rely on the 'theory_hours' and 'lab_hours' fields
 * to
 * allocate rooms and faculty.
 * - **Examination Board**: Uses these APIs to verify passing criteria during
 * result generation.
 * 
 * 🏗️ Design Intent:
 * Built as a **Modular API Layer**. Subjects are independent units that can be
 * mapped to multiple courses, and this controller provides the CRUD surface
 * to maintain that modularity.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/subjects")
@Tag(name = "Subject Management", description = "Professional APIs for defining and managing modular units of study (Subjects)")
@RequiredArgsConstructor
public class SubjectController {

    /**
     * Internal service managing subject lifecycle and credit logic.
     */
    private final CourseSubjectService subjectService;

    /**
     * 🆕 createSubject
     * 
     * 🛠️ Purpose: Defines a new area of study (e.g., 'Advanced Algorithms').
     * 🏛️ Business Rationale: The first step in building a syllabus is defining
     * the subject header.
     * 
     * @param dto Subject metadata including credits and contact hours.
     * @return ResponseEntity with the persisted subject.
     */
    @PostMapping
    @Operation(summary = "Define a new study module", description = "Creates a new Subject entity. Captures pedagogical details like credits, theory hours, and lab hours.", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    public ResponseEntity<SubjectResponseDTO> createSubject(@RequestBody SubjectRequestDTO dto) {
        return ResponseEntity.ok(subjectService.createSubject(dto));
    }

    /**
     * 📝 updateSubject
     * 
     * 🛠️ Purpose: Updates the parameters of an existing subject.
     * 🏛️ Business Rationale: Used when credit weightage or contact hours are
     * revised in the curriculum.
     */
    @PutMapping("/{subjectId}")
    @Operation(summary = "Update subject parameters", description = "Allows modification of credits, hour allocations, and descriptive metadata.", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    public ResponseEntity<SubjectResponseDTO> updateSubject(@PathVariable Long subjectId,
            @RequestBody SubjectRequestDTO dto) {
        return ResponseEntity.ok(subjectService.updateSubject(subjectId, dto));
    }

    /**
     * 🔍 getSubject
     * 
     * 🛠️ Purpose: Fetches the formal definition of a subject.
     * 🏛️ Business Rationale: Powers subject profile pages in the admin and student
     * portals.
     */
    @GetMapping("/{subjectId}")
    @Operation(summary = "Fetch subject definition", description = "Returns the full academic and administrative definition of a specific subject.", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    public ResponseEntity<SubjectResponseDTO> getSubject(@PathVariable Long subjectId) {
        return ResponseEntity.ok(subjectService.getSubject(subjectId));
    }

    /**
     * 🏢 getAllSubjectsByOrg
     * 
     * 🛠️ Purpose: Lists all study modules available in a specific institution.
     * 🏛️ Business Rationale: Used for syllabus mapping and course-subject
     * association wizards.
     */
    @GetMapping("/org/{orgId}")
    @Operation(summary = "List all subjects in an organization", description = "Fetches the master subject catalogue for a specific SaaS tenant.", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    public ResponseEntity<List<SubjectResponseDTO>> getAllSubjectsByOrg(@PathVariable Long orgId) {
        return ResponseEntity.ok(subjectService.getAllSubjectsByOrg(orgId));
    }

    /**
     * 🗑️ deleteSubject (Soft Delete)
     * 
     * 🛠️ Purpose: Marks a subject as no longer offered.
     * 🏛️ Business Rationale: Maintains data integrity for old syllabi that still
     * refer to this subject.
     */
    @DeleteMapping("/{subjectId}")
    @Operation(summary = "Deactivate a study module", description = "Performs a soft-delete (isActive=false). History is preserved to maintain references in historical syllabi.", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    public ResponseEntity<Void> deleteSubject(@PathVariable Long subjectId) {
        subjectService.deleteSubject(subjectId);
        return ResponseEntity.ok().build();
    }
}
