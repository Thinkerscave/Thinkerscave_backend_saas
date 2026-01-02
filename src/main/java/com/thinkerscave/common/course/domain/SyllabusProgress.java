package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.course.enums.ProgressStatus;
import com.thinkerscave.common.student.domain.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 📈 SyllabusProgress Entity - The Core Student Achievement Record
 * 
 * 🏛️ Business Purpose:
 * SyllabusProgress is the primary data source for tracking learning outcomes
 * within
 * the platform. It records the journey of a student through individual topics
 * of the curriculum. It transforms a static syllabus into a dynamic "Checklist
 * of Success", enabling automated milestone tracking and progress
 * visualization.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Students**: The owners of this progress; they update it to stay
 * organized.
 * - **Teachers**: Aggregate this data to see which students are falling behind
 * or which topics are taking longer to learn.
 * - **Parents**: View this data to provide home-level support for unfinished
 * topics.
 * - **Admins**: Use this for institutional-level "Course Coverage" audits.
 * 
 * 🔄 Academic Flow Position:
 * This is the **Active Delivery Layer**. It connects the "Published Curriculum"
 * with "Student Performance", serving as the bridge to final assessments.
 * 
 * 🏗️ Design Intent:
 * Built as a **Flat Fact Table** for high-performance reporting. By storing
 * 'chapter_id' and 'syllabus_id' directly (denormalization), we allow the
 * system
 * to generate complex progress reports with minimal database joins.
 */
@Entity
@Table(name = "syllabus_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SyllabusProgress extends Auditable {

    /**
     * Primary stable internal identifier for the progress event.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Long progressId;

    /**
     * The student whose learning journey is being tracked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Denormalized link to the subject for fast filtering in department-level
     * reports.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /**
     * Denormalized link to the syllabus version being followed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    private Syllabus syllabus;

    /**
     * Link to the module (Chapter) context.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    /**
     * The specific teachable unit (Topic) being tracked.
     * Hierarchy: Progress is tracked at the Topic level and rolled up.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    /**
     * Current state of understanding (NOT_STARTED, IN_PROGRESS, COMPLETED).
     * Workflow: Transitions are triggered by student or teacher actions.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ProgressStatus status = ProgressStatus.NOT_STARTED;

    /**
     * Numerical representation of progress (0-100).
     * Used for UI progress bars (e.g., "Math: 45% Complete").
     */
    @Column(name = "completion_percentage")
    private Integer completionPercentage;

    /**
     * Date when the student first interacted with this topic.
     * Analytics: Used to measure the "Learning Speed" of a student.
     */
    @Column(name = "started_date")
    private LocalDate startedDate;

    /**
     * Date when the topic was marked as finished.
     */
    @Column(name = "completed_date")
    private LocalDate completedDate;

    /**
     * Cumulative tally of time spent on this topic.
     * Business Rationale: Helps identify students who are struggling or
     * "Over-performing".
     */
    @Column(name = "time_spent_minutes")
    private Integer timeSpentMinutes;

    /**
     * Teacher's feedback or student's self-assessment notes (e.g., "Excellent
     * grasp").
     */
    @Column(columnDefinition = "TEXT")
    private String remarks;

    /**
     * Personal study notes captured by the student for this specific topic.
     * Extensibility: Encourages the student to use the platform as a study
     * companion.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
}
