package com.thinkerscave.common.course.service;

import com.thinkerscave.common.course.enums.ProgressStatus;

import java.util.Map;

/**
 * Service for tracking student progress on syllabus topics and chapters.
 */
public interface ProgressTrackingService {

    void updateTopicProgress(Long studentId, Long topicId, ProgressStatus status, Integer timeSpent, String remarks);

    double calculateSyllabusCompletionPercentage(Long studentId, Long syllabusId);

    Map<String, Object> getStudentProgressReport(Long studentId, Long syllabusId);

    void logSyllabusAccess(Long userId, Long syllabusId, String action);
}
