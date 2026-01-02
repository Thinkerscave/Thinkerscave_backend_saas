package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 📂 Chapter Entity - The Modular Building Blocks of Curriculum
 * 
 * 🏛️ Business Purpose:
 * Chapters act as the primary logical dividers within a syllabus. They group
 * related
 * educational concepts into digestible "Modules" (e.g., 'Organic Chemistry',
 * 'Ancient Societies'). This categorization is essential for high-level
 * academic
 * planning and helps teachers assign specific timeframes to broad subject
 * areas.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Academic Designers**: Group granular topics into these logical containers
 * to define the natural flow of learning.
 * - **Teachers**: Reference chapters to understand the thematic progression of
 * the semester.
 * - **Students**: Use chapter-level views to gauge their progress through major
 * sections of the course.
 * 
 * 🔄 Academic Flow Position:
 * Chapters represent the **Mid-Level Hierarchy**. They sit between the root
 * Syllabus (the whole book) and the Topics (the individual lessons).
 * 
 * 🏗️ Design Intent:
 * Built to be strictly sequential. The 'chapterNumber' determines the order of
 * delivery, while the 'learningObjectives' define the expected outcome for this
 * specific module.
 * 
 * 🚀 Future Extensibility:
 * - Chapter-level pre-requisites (Must finish Chapter 1 before Chapter 2).
 * - Aggregated performance analytics at the chapter level.
 * - Linking Chapter-level assessments (e.g., Module Tests).
 */
@Entity
@Table(name = "chapters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Chapter extends Auditable {

    /**
     * Unique stable identifier for the chapter record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chapter_id")
    private Long chapterId;

    /**
     * The thematic name of the module (e.g., "Introduction to Calculus").
     */
    @Column(name = "chapter_name", nullable = false, length = 255)
    private String chapterName;

    /**
     * Sequential position in the curriculum (1, 2, 3...).
     * Why: Vital for maintaining the intended pedagogical order across UI and
     * reports.
     */
    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    /**
     * The parent syllabus this chapter belongs to.
     * Relation: Many-to-One.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    private Syllabus syllabus;

    /**
     * Brief summary of what this module covers.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Estimated time required to complete the entire module.
     * Used for capacity planning and teacher schedule automation.
     */
    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    /**
     * Formal statement of what the student should know after finishing this
     * chapter.
     * Business Rationale: Used on official certificates and syllabus reports.
     */
    @Column(name = "learning_objectives", columnDefinition = "TEXT")
    private String learningObjectives;

    /**
     * The granular lessons (Topics) contained within this chapter.
     * Cascade Rule: Deleting a chapter removes all its nested topics.
     */
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Topic> topics = new ArrayList<>();

    /**
     * Availability flag.
     * Soft-deletion support to maintain historical referential integrity.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;
}
