package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.orgm.domain.Organisation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 🎓 Course Entity - The Foundation of Academic Certificates
 * 
 * 🏛️ Business Purpose:
 * The Course entity represents the primary educational products offered by an
 * institution. Whether it is a multi-year degree (B.Tech), a professional
 * certification (MBA), or a school grade (Grade 10), the Course defines the
 * macro-level boundaries of the student's journey, including its duration,
 * cost, and eligibility.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Prospective Students/Parents**: View Course details (fees, duration,
 * eligibility) to make enrollment decisions.
 * - **Registrar / Admissions**: Use this entity to validate applications
 * against
 * eligibility criteria.
 * - **Finance Department**: Relies on the 'fees' field to generate invoices and
 * manage revenue cycles.
 * - **Academic Planning**: Uses duration and semester data to schedule years of
 * institutional activity.
 * 
 * 🔄 Academic Flow Position:
 * This is the **Product Definition Layer**. A syllabus exists for a subject,
 * which in turn belongs to a Course. You cannot have a student without a
 * Course.
 * 
 * 🏗️ Design Intent:
 * Built to be highly flexible to support both **K-12 Schools** (where Courses
 * are 'Grades') and **Higher-Ed** (where Courses are 'Degrees'). It acts as
 * the root for all subject mappings and student enrollments.
 */
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Course extends Auditable {

    /**
     * Primary stable internal identifier for the course program.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    /**
     * Unique business-facing code (e.g., BTECH-CS-2024).
     * Used in public documents, IDs, and transcripts.
     */
    @Column(name = "course_code", unique = true, nullable = false, length = 50)
    private String courseCode;

    /**
     * Full name of the academic program (e.g., "Bachelor of Technology in Computer
     * Science").
     */
    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    /**
     * Comprehensive summary of the course objectives, scope, and outcomes.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Total span of the course in months.
     * Used for internal calculation of academic timelines.
     */
    @Column(name = "duration_months")
    private Integer durationMonths;

    /**
     * Human-readable duration in years (e.g., 4 years for B.Tech).
     * Primarily used for UI display and marketing.
     */
    @Column(name = "duration_years")
    private Integer durationYears;

    /**
     * Academic level (e.g., 'UG', 'PG', 'High School').
     * Relationship: Used to categorize courses for search and reporting.
     */
    @Column(name = "level", length = 50)
    private String level;

    /**
     * Specific qualification name (e.g., 'B.Tech', 'MBA', 'Grade 10').
     */
    @Column(name = "degree_type", length = 100)
    private String degreeType;

    /**
     * The owner organisation (SaaS Tenant) this course belongs to.
     * Hierarchy: Strict multi-tenant isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organisation organization;

    /**
     * Aggregate credits required to successfully graduate from this course.
     */
    @Column(name = "total_credits")
    private Integer totalCredits;

    /**
     * Minimum credit threshold for semester advancement.
     */
    @Column(name = "min_credits_required")
    private Integer minCreditsRequired;

    /**
     * Broad academic classification (e.g., 'Science', 'Arts', 'Technical').
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * Total number of academic periods/semesters across the entire duration.
     * Why: Used to generate student report cards and schedule terms.
     */
    @Column(name = "total_semesters")
    private Integer totalSemesters;

    /**
     * Flag to control if the course is currently open for enrollment.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * List of subjects included in this course curriculum.
     * Relation: Many-to-Many via the mapping entity.
     */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSubjectMapping> subjectMappings = new ArrayList<>();

    /**
     * Prerequisite requirements for a student to join this course.
     * Business Rationale: Displayed on the registration portal for lead
     * qualification.
     */
    @Column(name = "eligibility_criteria", columnDefinition = "TEXT")
    private String eligibilityCriteria;

    /**
     * Standard tuition fee for the entire course or per-year block.
     * Finance: The primary source of truth for generating student fee installments.
     */
    @Column(name = "fees")
    private Double fees;
}
