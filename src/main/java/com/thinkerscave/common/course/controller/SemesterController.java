package com.thinkerscave.common.course.controller;

import com.thinkerscave.common.course.dto.SemesterDTO;
import com.thinkerscave.common.course.service.SemesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🎓 SemesterController - Managing Academic Sessions
 * 
 * Defines the sub-units (Semesters/Trimesters) within an Academic Year.
 * Critical for course-subject mapping and grading periods.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/semesters")
@Tag(name = "Semester Management", description = "Manage semesters within academic years")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @PostMapping
    @Operation(summary = "Create a semester", description = "Adds a new semester to an academic year.")
    public ResponseEntity<SemesterDTO> createSemester(@RequestBody SemesterDTO dto) {
        return ResponseEntity.ok(semesterService.createSemester(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update semester", description = "Modify semester dates or details.")
    public ResponseEntity<SemesterDTO> updateSemester(@PathVariable Long id, @RequestBody SemesterDTO dto) {
        return ResponseEntity.ok(semesterService.updateSemester(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get semester details", description = "Fetch a specific semester by ID.")
    public ResponseEntity<SemesterDTO> getSemester(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.getSemester(id));
    }

    @GetMapping("/year/{academicYearId}")
    @Operation(summary = "List semesters for a year", description = "Get all semesters configured for a specific academic year.")
    public ResponseEntity<List<SemesterDTO>> getByYear(@PathVariable Long academicYearId) {
        return ResponseEntity.ok(semesterService.getSemestersByAcademicYear(academicYearId));
    }

    @GetMapping("/year/{academicYearId}/active")
    @Operation(summary = "List active semesters for a year", description = "Get only active semesters.")
    public ResponseEntity<List<SemesterDTO>> getActiveByYear(@PathVariable Long academicYearId) {
        return ResponseEntity.ok(semesterService.getActiveSemesters(academicYearId));
    }

    @GetMapping("/year/{academicYearId}/current")
    @Operation(summary = "Get current semester", description = "Fetch the currently running semester for the year.")
    public ResponseEntity<SemesterDTO> getCurrent(@PathVariable Long academicYearId) {
        return ResponseEntity.ok(semesterService.getCurrentSemester(academicYearId));
    }

    @PostMapping("/{id}/set-current")
    @Operation(summary = "Set active semester", description = "Marks this semester as the current active one, unmarking others in the same year.")
    public ResponseEntity<Void> setCurrent(@PathVariable Long id) {
        semesterService.setCurrentSemester(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete semester", description = "Soft delete a semester.")
    public ResponseEntity<Void> deleteSemester(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return ResponseEntity.ok().build();
    }
}
