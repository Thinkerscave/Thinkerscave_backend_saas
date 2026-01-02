package com.thinkerscave.common.course.controller;

import com.thinkerscave.common.course.dto.SyllabusRequestDTO;
import com.thinkerscave.common.course.dto.SyllabusResponseDTO;
import com.thinkerscave.common.course.enums.ProgressStatus;
import com.thinkerscave.common.course.service.ProgressTrackingService;
import com.thinkerscave.common.course.service.SyllabusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 🚀 Syllabus Management Controller
 * 
 * Purpose: Provides a public REST interface for managing curriculum lifecycle.
 * Business Problem: Enables collaboration between HODs, Teachers, and Students
 * by centralizing syllabus creation, approval, and progress monitoring.
 * 
 * Design Pattern: Standard RESTful resource mapping with status-driven
 * endpoints.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/syllabus")
@Tag(name = "Syllabus Management", description = "End-to-end curriculum lifecycle including versioning and student progress tracking")
@RequiredArgsConstructor
public class SyllabusController {

    private final SyllabusService syllabusService;
    private final ProgressTrackingService progressService;

    /**
     * Entry point for new curriculum creation.
     * Starts in DRAFT status for review.
     */
    @PostMapping
    @Operation(summary = "Create a new syllabus draft", description = "Initializes a subject curriculum blueprint. Content starts as a Draft.")
    public ResponseEntity<SyllabusResponseDTO> createSyllabus(@RequestBody SyllabusRequestDTO dto) {
        return ResponseEntity.ok(syllabusService.createSyllabus(dto));
    }

    /**
     * Modifies current draft.
     * Why: Enforces Draft-only updates to prevent corruption of published history.
     */
    @PutMapping("/{syllabusId}")
    @Operation(summary = "Update a syllabus draft", description = "Modifies existing draft content. Will fail if status is already Approved or Published.")
    public ResponseEntity<SyllabusResponseDTO> updateSyllabus(@PathVariable Long syllabusId,
            @RequestBody SyllabusRequestDTO dto) {
        return ResponseEntity.ok(syllabusService.updateSyllabus(syllabusId, dto));
    }

    /**
     * Detailed retrieval including deep nesting (Chapters -> Topics).
     */
    @GetMapping("/{syllabusId}")
    @Operation(summary = "Get syllabus details", description = "Returns the full hierarchy of a specific syllabus version.")
    public ResponseEntity<SyllabusResponseDTO> getSyllabus(@PathVariable Long syllabusId) {
        return ResponseEntity.ok(syllabusService.getSyllabus(syllabusId));
    }

    /**
     * Main endpoint for student/teacher dashboards.
     */
    @GetMapping("/subject/{subjectId}/latest")
    @Operation(summary = "Get latest curriculum for a subject", description = "Fetches the highest version number available for a given subject.")
    public ResponseEntity<SyllabusResponseDTO> getLatestSyllabus(@PathVariable Long subjectId) {
        return ResponseEntity.ok(syllabusService.getLatestSyllabusBySubject(subjectId));
    }

    /**
     * Workflow transition: Draft -> Approved.
     */
    @PostMapping("/{syllabusId}/approve")
    @Operation(summary = "Approve a syllabus", description = "Officially marks the content as reviewed. Requires a valid User ID for audit logs.")
    public ResponseEntity<Void> approveSyllabus(@PathVariable Long syllabusId, @RequestParam Long userId) {
        syllabusService.approveSyllabus(syllabusId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Workflow transition: Approved -> Published.
     * Why: Separating Approve from Publish allows for scheduling or batch
     * releasing.
     */
    @PostMapping("/{syllabusId}/publish")
    @Operation(summary = "Publish an approved syllabus", description = "Makes the curriculum live for students and teachers.")
    public ResponseEntity<Void> publishSyllabus(@PathVariable Long syllabusId) {
        syllabusService.publishSyllabus(syllabusId);
        return ResponseEntity.ok().build();
    }

    /**
     * Versioning trigger.
     * Business Logic: Clones the old syllabus into a new draft to save
     * administrative time.
     */
    @PostMapping("/{syllabusId}/version")
    @Operation(summary = "Create a new curriculum version", description = "Deep-clones an existing syllabus into a new Draft for a new year or semester.")
    public ResponseEntity<SyllabusResponseDTO> createNewVersion(@PathVariable Long syllabusId,
            @RequestParam String newVersion) {
        return ResponseEntity.ok(syllabusService.createNewVersion(syllabusId, newVersion));
    }

    // --- Progress Tracking Section ---

    /**
     * Updates topic-level completion status.
     * Used by Teachers to mark what has been taught, or by Students for self-study
     * tracking.
     */
    @PostMapping("/progress/topic/{topicId}")
    @Operation(summary = "Log topic completion", description = "Tracks how much of the syllabus has been completed by a specific student.")
    public ResponseEntity<Void> updateProgress(
            @PathVariable Long topicId,
            @RequestParam Long studentId,
            @RequestParam ProgressStatus status,
            @RequestParam(required = false) Integer timeSpent,
            @RequestParam(required = false) String remarks) {
        progressService.updateTopicProgress(studentId, topicId, status, timeSpent, remarks);
        return ResponseEntity.ok().build();
    }

    /**
     * Analytical view of a student's learning journey.
     */
    @GetMapping("/progress/student/{studentId}/syllabus/{syllabusId}")
    @Operation(summary = "Fetch student progress report", description = "Calculates percentage of completion and details per topic.")
    public ResponseEntity<Map<String, Object>> getProgressReport(
            @PathVariable Long studentId,
            @PathVariable Long syllabusId) {
        return ResponseEntity.ok(progressService.getStudentProgressReport(studentId, syllabusId));
    }

    @PostMapping("/{syllabusId}/access-log")
    @Operation(summary = "Log access activity", description = "Security audit for tracking who viewed the syllabus content.")
    public ResponseEntity<Void> logAccess(
            @PathVariable Long syllabusId,
            @RequestParam Long userId,
            @RequestParam String action) {
        progressService.logSyllabusAccess(userId, syllabusId, action);
        return ResponseEntity.ok().build();
    }
}
