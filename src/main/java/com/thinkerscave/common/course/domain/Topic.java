package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 📍 Topic Entity - The Fundamental Unit of Learning
 * 
 * 🏛️ Business Purpose:
 * Topics are the most granular units of educational content in ThinkersCave.
 * They represent individual "Lessons" or "Teachable Sessions" (e.g., 'Laws of
 * Thermodynamics', 'French Verb Conjugation'). This level of detail is critical
 * for providing students with a sense of daily achievement and giving teachers
 * a way to precisely roadmap their delivery.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Teachers**: The primary "Planers" who use topics to schedule daily
 * lessons.
 * - **Students**: The "Trackers" who mark these as finished to see their
 * learning
 * percentage grow in real-time.
 * - **Parents**: Rely on topic-level progress to understand exactly what their
 * child learned today.
 * 
 * 🔄 Academic Flow Position:
 * Topics represent the **Execution Layer**. All actual student progress
 * tracking
 * occurs at this level, which then rolls up into chapter and syllabus
 * aggregates.
 * 
 * 🏗️ Design Intent:
 * Focused on **actionability**. Includes fields for 'estimatedHours' to help
 * with session scheduling and 'contentUrl' to bridge the gap between curriculum
 * planning and actual learning materials.
 * 
 * 🚀 Future Extensibility:
 * - Tagging topics with "Competencies" or "Skills".
 * - Linking automated quiz banks to specific topic IDs.
 * - Support for "Optional" vs "Mandatory" topics.
 */
@Entity
@Table(name = "topics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Topic extends Auditable {

    /**
     * Stable unique identifier for the lesson/topic entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    private Long topicId;

    /**
     * Clear, descriptive name of the lesson (e.g., "Photosynthesis Mechanism").
     */
    @Column(name = "topic_name", nullable = false, length = 255)
    private String topicName;

    /**
     * Sequential order of the topic within its parent chapter.
     * Vital for displaying lessons in the correct pedagogical sequence.
     */
    @Column(name = "topic_number", nullable = false)
    private Integer topicNumber;

    /**
     * The parent module (Chapter) this topic belongs to.
     * Relation: Many-to-One.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    /**
     * Detailed breakdown of what will be covered in this specific lesson.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Targeted duration for teaching this specific topic.
     * Why: Helps the system automatically calculate if the syllabus can be
     * finished within the allocated semester dates.
     */
    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    /**
     * Link to external or internal learning materials (PDFs, PPTs, Videos).
     * Why: Seamlessly connects "What to learn" with "Where to learn".
     */
    @Column(name = "content_url", length = 500)
    private String contentUrl;

    /**
     * Availability flag for archival and lifecycle management.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;
}
