package com.thinkerscave.common.course.controller;

import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.dto.AcademicContainerDTO;
import com.thinkerscave.common.course.service.AcademicStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🏛️ AcademicStructureController - The Institutional Foundation API
 * 
 * 🏛️ Business Purpose:
 * This controller serves as the "Master Blueprint" interface for an
 * institution.
 * It manages the dual-backbone of the platform: The Academic Calendar (Years)
 * and the Organizational Hierarchy (Containers like Classes and Sections).
 * It enables the system to adapt to any educational model—from standard schools
 * to multi-campus universities.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Institutional Owners / Super Admins**: Use these APIs for high-level
 * setup
 * during the onboarding phase.
 * - **Registrars**: Manage the year-on-year transition of the academic
 * calendar.
 * - **Frontend Architects**: Consume these recursive endpoints to build
 * multi-level navigation trees in the dashboard.
 * 
 * 🏗️ Design Intent:
 * Focused on **Scalability and Automation**. The controller includes
 * "Generation"
 * utilities that can instantly spin up a standard school or college structure,
 * significantly reducing the 'Time-to-Value' for new SaaS tenants.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/academic-structure")
@Tag(name = "Academic Structure Management", description = "Professional APIs for institutional hierarchy, academic calendar, and bulk structural generation")
@RequiredArgsConstructor
public class AcademicStructureController {

    /**
     * Internal service managing recursive structural logic and calendar state.
     */
    private final AcademicStructureService structureService;

    /**
     * 📅 createAcademicYear
     * 
     * 🛠️ Purpose: Defines a new time-cycle for the institution.
     * 👥 Triggered by: Administrator (System Configuration).
     * 
     * @param orgId     The tenant's ID.
     * @param yearCode  Unique business code (e.g., AY2024-25).
     * @param startDate Isolation point for session beginning.
     * @param endDate   Isolation point for session closure.
     * @return ResponseEntity with the created AcademicYear.
     */
    @PostMapping("/years")
    @Operation(summary = "Initialize a new academic year", description = "Registers a new calendar cycle for the institution. Used during year-end preparation.")
    public ResponseEntity<AcademicYear> createAcademicYear(
            @RequestParam Long orgId,
            @RequestParam String yearCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(structureService.createAcademicYear(orgId, yearCode, startDate, endDate));
    }

    /**
     * 🔍 getAcademicYears
     * 
     * 🛠️ Purpose: Lists all historical and future sessions.
     * 🏛️ Business Rationale: Required for alumni checks and planning future
     * intakes.
     */
    @GetMapping("/years/{orgId}")
    @Operation(summary = "Fetch institutional calendar history", description = "Lists all registered academic years for a specific organization.")
    public ResponseEntity<List<AcademicYear>> getAcademicYears(@PathVariable Long orgId) {
        return ResponseEntity.ok(structureService.getAcademicYears(orgId));
    }

    /**
     * 📍 getCurrentYear
     * 
     * 🛠️ Purpose: Identifies the active academic session.
     * 🏛️ Business Rationale: Used by the mobile and web portals to default the
     * user's view to current data (Timetables, Syllabus).
     */
    @GetMapping("/years/{orgId}/current")
    @Operation(summary = "Identify currently active session", description = "Returns the single academic year marked as 'Current' for the organization.")
    public ResponseEntity<AcademicYear> getCurrentYear(@PathVariable Long orgId) {
        return ResponseEntity.ok(structureService.getCurrentAcademicYear(orgId));
    }

    /**
     * 🔄 setCurrentYear
     * 
     * 🛠️ Purpose: Switches the platform's active state to a new year.
     * 🏛️ Business Rationale: Performed by the Admin exactly once a year during
     * the session transition phase.
     */
    @PostMapping("/years/{orgId}/current/{yearId}")
    @Operation(summary = "Switch the active academic session", description = "Updates the organization's state to treat the specified year as the primary active context.")
    public ResponseEntity<Void> setCurrentYear(@PathVariable Long orgId, @PathVariable Long yearId) {
        structureService.setCurrentAcademicYear(orgId, yearId);
        return ResponseEntity.ok().build();
    }

    /**
     * 🆕 createContainer
     * 
     * 🛠️ Purpose: Adds a single node to the institutional hierarchy.
     * 🏛️ Business Rationale: Used for granular adjustments (e.g., adding a
     * single new 'Section C' to 'Grade 10').
     */
    @PostMapping("/containers")
    @Operation(summary = "Add a new structural node", description = "Creates an Academic Container (Class, Section, Branch, etc.). Supports hierarchy via parentContainerId.")
    public ResponseEntity<AcademicContainerDTO> createContainer(@RequestBody AcademicContainerDTO dto) {
        return ResponseEntity.ok(structureService.createContainer(dto));
    }

    /**
     * 📝 updateContainer
     * 
     * 🛠️ Purpose: Updates technical parameters of a container (e.g., capacity).
     */
    @PutMapping("/containers/{containerId}")
    @Operation(summary = "Update structural node parameters", description = "Allows modification of container name, capacity, and display order.")
    public ResponseEntity<AcademicContainerDTO> updateContainer(@PathVariable Long containerId,
            @RequestBody AcademicContainerDTO dto) {
        return ResponseEntity.ok(structureService.updateContainer(containerId, dto));
    }

    /**
     * 🔍 getContainer
     * 
     * 🛠️ Purpose: Fetches data for a specific class or section.
     */
    @GetMapping("/containers/{containerId}")
    @Operation(summary = "Fetch container profile", description = "Returns the full descriptive profile of a specific structural node.")
    public ResponseEntity<AcademicContainerDTO> getContainer(@PathVariable Long containerId) {
        return ResponseEntity.ok(structureService.getContainer(containerId));
    }

    /**
     * 🔝 getTopLevelContainers
     * 
     * 🛠️ Purpose: Entry point for the institutional tree view.
     * 🏛️ Business Rationale: Powers the sidebar navigation (e.g., showing a
     * list of all 'Grades').
     */
    @GetMapping("/containers/org/{orgId}/year/{yearId}")
    @Operation(summary = "List root structural nodes", description = "Fetches all top-level containers (where parent is null) for an organization's specific year.")
    public ResponseEntity<List<AcademicContainerDTO>> getTopLevelContainers(
            @PathVariable Long orgId,
            @PathVariable Long yearId) {
        return ResponseEntity.ok(structureService.getTopLevelContainers(orgId, yearId));
    }

    /**
     * 👶 getChildContainers
     * 
     * 🛠️ Purpose: Enables "Drill-Down" navigation in the UI.
     * 🏛️ Business Rationale: Used when a user clicks a Grade to see its Sections.
     */
    @GetMapping("/containers/{parentId}/children")
    @Operation(summary = "Drill-down into sub-containers", description = "Returns all immediate child nodes of the provided parent container.")
    public ResponseEntity<List<AcademicContainerDTO>> getChildContainers(@PathVariable Long parentId) {
        return ResponseEntity.ok(structureService.getChildContainers(parentId));
    }

    /**
     * 🏭 generateSchoolStructure (Bulk Setup)
     * 
     * 🛠️ Purpose: Radical reduction in onboarding time for K-12 institutions.
     * 🏛️ Business Rationale: Instantly creates Grades 1-12 and their child
     * sections via a single API call.
     */
    @PostMapping("/generate-school")
    @Operation(summary = "Batch-generate K-12 school structure", description = "Automates the creation of a standard school hierarchy (Classes 1-10 with Section A).")
    public ResponseEntity<Void> generateSchoolStructure(@RequestParam Long orgId, @RequestParam Long yearId) {
        structureService.generateSchoolStructure(orgId, yearId);
        return ResponseEntity.ok().build();
    }

    /**
     * 🎓 generateCollegeStructure (Bulk Setup)
     * 
     * 🛠️ Purpose: Adapts the system to a Higher-Ed model.
     * 🏛️ Business Rationale: Quickly maps a Course to its nested Year-based
     * branch structure.
     */
    @PostMapping("/generate-college")
    @Operation(summary = "Batch-generate higher-ed branch structure", description = "Automates the creation of a college branch and its duration-based years for a specific course.")
    public ResponseEntity<Void> generateCollegeStructure(
            @RequestParam Long orgId,
            @RequestParam Long yearId,
            @RequestParam Long courseId) {
        structureService.generateCollegeStructure(orgId, yearId, courseId);
        return ResponseEntity.ok().build();
    }

    /**
     * 🗑️ deleteContainer (Soft Delete)
     * 
     * 🛠️ Purpose: Decommissions a part of the hierarchy.
     */
    @DeleteMapping("/containers/{containerId}")
    @Operation(summary = "Retire a structural node", description = "Deletes the container. WARNING: Hard-delete in current implementation. Should only be used if no students are assigned.")
    public ResponseEntity<Void> deleteContainer(@PathVariable Long containerId) {
        structureService.deleteContainer(containerId);
        return ResponseEntity.ok().build();
    }
}
