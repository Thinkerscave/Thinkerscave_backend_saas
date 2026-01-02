package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.course.enums.SubjectCategory;
import com.thinkerscave.common.orgm.domain.Organisation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 📚 Subject Entity - The Modular Units of Knowledge
 * 
 * 🏛️ Business Purpose:
 * Subjects are the fundamental "building blocks" of an academic curriculum.
 * While a Course describes a whole program, a Subject captures the technical
 * parameters of a single area of study (e.g., 'Data Structures', 'World
 * History').
 * It defines the credits, the workload (hours), and the evaluation criteria
 * (passing marks) for that module.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Teachers / Subject Matter Experts (SMEs)**: Define why the subject is
 * needed and build the syllabus on top of it.
 * - **Students**: Focus on individual subjects to earn credits toward their
 * degree.
 * - **Examination Department**: Uses 'maxMarks' and 'passingMarks' to configure
 * exam papers and grading logic.
 * - **Librarians**: Use subject codes to categorize learning resources in the
 * library.
 * 
 * 🔄 Academic Flow Position:
 * This is the **Middle-Level Definition**. Subjects are mapped to Courses and
 * syllabi are built specifically to deliver the content of a Subject.
 * 
 * 🏗️ Design Intent:
 * Designed to separate the **Definition of a Subject** from the **Delivery of a
 * Subject (Syllabus)**. This allows an institution to have one target 'Physics'
 * subject but multiple ways to teach it via different syllabus versions.
 * 
 * 🚀 Future Extensibility:
 * - Mapping subjects to "Skill Tags" for career path analysis.
 * - Automated resource recommendations based on subject categories.
 * - Cross-course subject reusability (Elective subjects).
 */
@Entity
@Table(name = "subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Subject extends Auditable {

    /**
     * Primary stable internal identifier for the subject record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    private Long subjectId;

    /**
     * Unique academic code (e.g., CS-101, MATH-202).
     * Purpose: Standardized reference used in transcripts and external exams.
     */
    @Column(name = "subject_code", unique = true, nullable = false, length = 50)
    private String subjectCode;

    /**
     * Full title of the subject (e.g., "Advanced Differential Equations").
     */
    @Column(name = "subject_name", nullable = false, length = 255)
    private String subjectName;

    /**
     * High-level summary of the subject's scope and pedagogical goals.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Academic weightage of the subject.
     * Why: Determines the impact of this subject on the student's CGPA Calculation.
     */
    @Column(name = "credits")
    private Integer credits;

    /**
     * Broad classification (e.g., CORE, ELECTIVE, VOCATIONAL).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private SubjectCategory category;

    /**
     * Targeted classroom teaching hours.
     * Used for resource planning and classroom scheduling.
     */
    @Column(name = "theory_hours")
    private Integer theoryHours;

    /**
     * Targeted hands-on or real-world application hours.
     */
    @Column(name = "practical_hours")
    private Integer practicalHours;

    /**
     * Targeted laboratory or computer-based practice hours.
     */
    @Column(name = "lab_hours")
    private Integer labHours;

    /**
     * The tenant organisation this subject belongs to.
     * Multi-tenancy: Strictly siloed by institutional ID.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organisation organization;

    /**
     * Availability flag for active/inactive subject management.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * History of all syllabus versions built for this subject.
     * Relation: One-to-Many.
     */
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Syllabus> syllabi = new ArrayList<>();

    /**
     * Total possible points for this subject's evaluation.
     * Exam Module: Used as the denominator in result calculations.
     */
    @Column(name = "max_marks")
    private Integer maxMarks;

    /**
     * Minimum score required to be considered 'Passed' or 'Competent'.
     */
    @Column(name = "passing_marks")
    private Integer passingMarks;
}
