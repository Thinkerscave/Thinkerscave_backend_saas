package com.thinkerscave.common.course.service;

import com.thinkerscave.common.course.dto.SyllabusRequestDTO;
import com.thinkerscave.common.course.dto.SyllabusResponseDTO;

import java.util.List;

/**
 * 🛠️ Syllabus Service Interface
 * 
 * Core responsibilities:
 * - Handling the lifecycle of a syllabus (Draft -> Published).
 * - Managing hierarchical content (Chapters/Topics).
 * - Supporting versioning flows to maintain academic history.
 */
public interface SyllabusService {

    /**
     * Creates a new syllabus in DRAFT mode.
     * Business Rule: A new syllabus is always created as a draft for review.
     */
    SyllabusResponseDTO createSyllabus(SyllabusRequestDTO dto);

    /**
     * Updates an existing syllabus.
     * Business Limitation: Only syllabi in DRAFT status can be modified to maintain
     * integrity.
     */
    SyllabusResponseDTO updateSyllabus(Long syllabusId, SyllabusRequestDTO dto);

    /**
     * Fetches detailed syllabus information including chapters and topics.
     */
    SyllabusResponseDTO getSyllabus(Long syllabusId);

    /**
     * Retrieves the most recently PUBLISHED syllabus for a given subject.
     * Primarily used by students and teachers to see the current active curriculum.
     */
    SyllabusResponseDTO getLatestSyllabusBySubject(Long subjectId);

    /**
     * Lists all syllabus versions (Draft, Approved, etc.) for a subject.
     */
    List<SyllabusResponseDTO> getAllSyllabiBySubject(Long subjectId);

    /**
     * Transitions a syllabus to APPROVED status.
     * Requires an authorized User ID for audit logs.
     */
    void approveSyllabus(Long syllabusId, Long userId);

    /**
     * Makes a syllabus available for teaching by setting status to PUBLISHED.
     * Business Logic: Only one syllabus per subject/academic year should be
     * published.
     */
    void publishSyllabus(Long syllabusId);

    /**
     * Clones an existing syllabus into a new version.
     * Why: Allows quick curriculum updates for a new academic year without rework.
     */
    SyllabusResponseDTO createNewVersion(Long oldSyllabusId, String newVersion);

    /**
     * Permanently simplifies the system by removing a syllabus.
     * Note: Should be used with caution as it may break progress tracking
     * references.
     */
    void deleteSyllabus(Long syllabusId);
}
