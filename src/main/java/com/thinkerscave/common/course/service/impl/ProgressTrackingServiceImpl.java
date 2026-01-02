package com.thinkerscave.common.course.service.impl;

import com.thinkerscave.common.course.domain.*;
import com.thinkerscave.common.course.enums.ProgressStatus;
import com.thinkerscave.common.course.repository.*;
import com.thinkerscave.common.course.service.ProgressTrackingService;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.repository.StudentRepository;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🎓 ProgressTrackingServiceImpl - The Student Learning Analytics Hub
 * 
 * 🏛️ Business Purpose:
 * This service is the "Pulse" of the educational journey in ThinkersCave. It
 * provides
 * real-time visibility into how students are interacting with the published
 * curriculum.
 * By tracking completion at the granular Topic level, it enables data-driven
 * insights
 * for educators and motivation for learners.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Students**: Update their own progress to stay organized and motivated.
 * - **Teachers / Tutors**: Monitor aggregate class progress to identify topics
 * that
 * need revision or additional time.
 * - **Parents**: View detailed progress reports to stay informed about their
 * child's
 * academic coverage.
 * - **Principals / Admins**: Use these analytics to verify that the syllabus is
 * being delivered according to the academic calendar.
 * 
 * 🔄 Academic Flow Position:
 * This service operates during the **Execution Phase** of the academic cycle.
 * It
 * transforms the static Syllabus into a dynamic roadmap of completed
 * milestones.
 * 
 * 🏗️ Design Intent:
 * Built with an **Event-Driven Audit** mindset. Every interaction is either a
 * state change (Planned -> Finished) or an access log, providing a rich history
 * for future AI-driven learning recommendations.
 * 
 * 🚀 Future Extensibility:
 * - Integration with digital assessments (Auto-completing topics upon passing a
 * quiz).
 * - "Time-to-Completion" predictions based on historical time-spent data.
 * - Gamification (Badges/Certificates awarded based on percentage milestones).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressTrackingServiceImpl implements ProgressTrackingService {

        /**
         * Primary repository for student progress records.
         * Stores the state of completion for every student-topic pair.
         */
        private final SyllabusProgressRepository progressRepository;

        /**
         * Audit repository for curriculum access.
         * Tracks who viewed what, and when—essential for security and engagement
         * analysis.
         */
        private final SyllabusAccessRepository accessRepository;

        /**
         * Repository for Student data.
         * Ensures every progress record is linked to a valid enrolled learner.
         */
        private final StudentRepository studentRepository;

        /**
         * Repository for Topics.
         * The target milestone being tracked.
         */
        private final TopicRepository topicRepository;

        /**
         * Repository for Syllabi.
         * Used for calculating aggregate completion percentages.
         */
        private final SyllabusRepository syllabusRepository;

        /**
         * Repository for Users.
         * Specifically for logging access by staff, admins, or students.
         */
        private final UserRepository userRepository;

        /**
         * 📈 updateTopicProgress
         * 
         * 🛠️ Purpose: Updates the learning status of a specific topic for a student.
         * ⏰ When it is called: When a student finishes a lesson or a teacher marks
         * a topic as covered.
         * 👤 Triggered by: Student App / Teacher Portal.
         * 
         * @param studentId The learner's ID.
         * @param topicId   The subject module being tracked.
         * @param status    The new state (DRAFT, IN_PROGRESS, COMPLETED).
         * @param timeSpent Duration in minutes (optional).
         * @param remarks   Qualitative feedback (e.g., "Need more practice on this").
         * 
         *                  ⚠️ Side Effects: Updates or inserts into the
         *                  'syllabus_progress' table.
         */
        @Override
        @Transactional
        public void updateTopicProgress(Long studentId, Long topicId, ProgressStatus status, Integer timeSpent,
                        String remarks) {
                // Validation: Fail-fast if entities are missing to avoid orphaned progress
                // data.
                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
                Topic topic = topicRepository.findById(topicId)
                                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

                // Logical check: Find existing record or start a new tracking journey for this
                // topic.
                SyllabusProgress progress = progressRepository.findByStudentAndTopic(student, topic)
                                .orElse(new SyllabusProgress());

                if (progress.getProgressId() == null) {
                        // New record initialization: Link the entire hierarchy for easy flat-table
                        // reporting.
                        progress.setStudent(student);
                        progress.setTopic(topic);
                        progress.setChapter(topic.getChapter());
                        progress.setSyllabus(topic.getChapter().getSyllabus());
                        progress.setSubject(topic.getChapter().getSyllabus().getSubject());
                        progress.setStartedDate(LocalDate.now());
                }

                progress.setStatus(status);
                progress.setRemarks(remarks);

                // Cumulative Metrics: Accumulate time spent over multiple study sessions.
                if (timeSpent != null) {
                        progress.setTimeSpentMinutes(
                                        (progress.getTimeSpentMinutes() != null ? progress.getTimeSpentMinutes() : 0)
                                                        + timeSpent);
                }

                // Logic Rationale: Hardcoded weightage for progress states.
                // Completed = 100%, In-Progress = 50%, Planned = 0%.
                if (status == ProgressStatus.COMPLETED) {
                        progress.setCompletedDate(LocalDate.now());
                        progress.setCompletionPercentage(100);
                } else if (status == ProgressStatus.IN_PROGRESS) {
                        progress.setCompletionPercentage(50);
                } else {
                        progress.setCompletionPercentage(0);
                }

                progressRepository.save(progress);
                log.info("Progress updated for Student: {} on Topic: {}. Status: {}",
                                studentId, topic.getTopicName(), status);
        }

        /**
         * 🧮 calculateSyllabusCompletionPercentage
         * 
         * 🛠️ Purpose: Calculates a weighted average of curriculum coverage.
         * 📊 Formula: (Count of Completed Topics / Total Topics in Syllabus) * 100.
         * 
         * @return A double value representing the percentage (0.0 to 100.0).
         */
        @Override
        public double calculateSyllabusCompletionPercentage(Long studentId, Long syllabusId) {
                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
                Syllabus syllabus = syllabusRepository.findById(syllabusId)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

                // Step 1: Count total work content.
                long totalTopics = syllabus.getChapters().stream()
                                .flatMap(chapter -> chapter.getTopics().stream())
                                .count();

                // Edge Case: Return zero if the syllabus has no content to avoid division by
                // zero.
                if (totalTopics == 0)
                        return 0;

                // Step 2: Count successfully delivered content.
                long completedTopics = progressRepository.findByStudentAndSyllabus(student, syllabus).stream()
                                .filter(p -> p.getStatus() == ProgressStatus.COMPLETED)
                                .count();

                return (double) completedTopics * 100 / totalTopics;
        }

        /**
         * 📄 getStudentProgressReport
         * 
         * 🛠️ Purpose: Generates a comprehensive view of a student's standing.
         * 👤 Who triggers it: Parents/Teachers during PTMs (Parent Teacher Meetings).
         */
        @Override
        public Map<String, Object> getStudentProgressReport(Long studentId, Long syllabusId) {
                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
                Syllabus syllabus = syllabusRepository.findById(syllabusId)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

                List<SyllabusProgress> progressList = progressRepository.findByStudentAndSyllabus(student, syllabus);

                // Data Assembly: Constructing a flat report for UI consumption.
                Map<String, Object> report = new HashMap<>();
                report.put("studentName", student.getFirstName() + " " + student.getLastName());
                report.put("syllabusTitle", syllabus.getTitle());
                report.put("overallCompletion", calculateSyllabusCompletionPercentage(studentId, syllabusId));

                // Logical Map: TopicID -> Status String.
                // This allows the UI to easily highlight topic nodes in a tree/list view.
                Map<Long, String> topicStatusMap = new HashMap<>();
                progressList.forEach(p -> topicStatusMap.put(p.getTopic().getTopicId(), p.getStatus().name()));
                report.put("topicProgress", topicStatusMap);

                return report;
        }

        /**
         * 👁️ logSyllabusAccess
         * 
         * 🛠️ Purpose: Audit logging for curriculum interaction.
         * 🏛️ Business Rationale: Required for security compliance and to detect
         * if students are actually looking at materials.
         * 
         * @param action The nature of the interaction (e.g., "VIEWED", "DOWNLOADED").
         */
        @Override
        @Transactional
        public void logSyllabusAccess(Long userId, Long syllabusId, String action) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Syllabus syllabus = syllabusRepository.findById(syllabusId)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

                // Upsert Logic: Track unique user-syllabus engagement.
                SyllabusAccess access = accessRepository.findByUserAndSyllabus(user, syllabus)
                                .orElse(new SyllabusAccess());

                if (access.getAccessId() == null) {
                        access.setUser(user);
                        access.setSyllabus(syllabus);
                        access.setViewCount(0);
                }

                // Increment engagement metrics.
                access.setViewCount(access.getViewCount() + 1);
                access.setLastAccessedDate(LocalDateTime.now());
                access.setLastAction(action);

                accessRepository.save(access);
        }
}
