package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.course.enums.SyllabusStatus;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 📘 Syllabus Entity - The Educational Blueprint of ThinkersCave
 * 
 * 🏛️ Business Purpose:
 * The Syllabus entity acts as the definitive roadmap for professional
 * curriculum delivery.
 * It standardizes educational content across an institution, ensuring that
 * every
 * teacher follows the same structured path toward learning objectives. It
 * solvesthe
 * problem of "Information Silos" where teaching methods vary too widely between
 * different faculty members.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Admin / HOD**: The "Master Architects" who design the curriculum and
 * enforce quality standards via the approval workflow.
 * - **Teachers**: The "Delivery Leads" who use this as their daily guide for
 * lesson planning and progress reporting.
 * - **Students**: The "Target Audience" who view this to understand their
 * learning
 * expectations and track their academic coverage.
 * - **Government / Accreditation Bodies**: Reports generated from this entity
 * prove that the institution is following mandated educational standards.
 * 
 * 🔄 Academic Flow Position:
 * Situated at the heart of the "Curriculum Phase". It bridges the gap between
 * high-level academic theory (Subjects) and real-time student interaction
 * (Progress Tracking).
 * 
 * 🏗️ Design Intent:
 * Designed for **strict version control** and **hierarchical depth**. It
 * supports
 * the 'Draft -> Approved -> Published' lifecycle to ensure that changes are
 * reviewed by management before becoming visible to the student body.
 * 
 * 🚀 Future Extensibility:
 * - Direct linking of digital assets (PDFs, Videos) to specific topics.
 * - AI mapping of syllabus topics to global skill sets.
 * - Multi-lingual support for regional curricula.
 */
@Entity
@Table(name = "syllabus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Syllabus extends Auditable {

    /**
     * Primary stable identifier for the syllabus entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "syllabus_id")
    private Long syllabusId;

    /**
     * Unique Business Identifier (e.g., SYL-MATH10-2024).
     * Why: This is a professional code used in physical reports and cross-system
     * integrations where database IDs are not suitable or secure.
     */
    @Column(name = "syllabus_code", unique = true, length = 50)
    private String syllabusCode;

    /**
     * Descriptive name of the curriculum version (e.g., "Standard Mathematics v2").
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * The subject context (Physics, History, etc.) this curriculum is built for.
     * Relation: Many-to-One. Multiple syllabus versions can exist for a single
     * subject.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /**
     * The calendar cycle this syllabus is intended for.
     * Why: Prevents using an outdated curriculum for a new academic intake.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    /**
     * High-level summary of the learning objectives and purpose of this syllabus.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Calculated or estimated total hours required to complete this curriculum.
     * Used for scheduling teacher workloads and resource planning.
     */
    @Column(name = "total_hours")
    private Integer totalHours;

    /**
     * Availability flag for soft-deletion and archival.
     * Business Rule: Historical syllabi are never hard-deleted; they are just
     * marked inactive to preserve student progress history.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * The hierarchical modules (Chapters) that make up this syllabus.
     * Relationship: One-to-Many. Deleting a syllabus triggers a cascade
     * deletion of its entire content tree.
     */
    @OneToMany(mappedBy = "syllabus", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters = new ArrayList<>();

    /**
     * Human-readable version number (e.g., "1.0", "2024.1").
     * Essential for tracking curriculum evolution over several years.
     */
    @Column(name = "version", length = 20)
    private String version;

    /**
     * Current workflow state (DRAFT, APPROVED, PUBLISHED).
     * Standardizes the lifecycle management from planning to delivery.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private SyllabusStatus status = SyllabusStatus.DRAFT;

    /**
     * Link to the ancestor version.
     * Why: Allows administrators to trace what changed between versions for
     * transparency and audit reports.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_version_id")
    private Syllabus previousVersion;

    /**
     * The authorized individual who cleared this curriculum for use.
     * Security: Logs the identity of the person responsible for the academic
     * quality.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    /**
     * Date stamp of official validation.
     */
    @Column(name = "approved_date")
    private LocalDate approvedDate;

    /**
     * Date when the syllabus was made visible to students and teachers.
     */
    @Column(name = "published_date")
    private LocalDate publishedDate;

    /**
     * Date when this curriculum was taken out of active rotation.
     */
    @Column(name = "archived_date")
    private LocalDate archivedDate;

    /**
     * Qualitative feedback or reasoning from the reviewer during the approval
     * process.
     */
    @Column(name = "approval_remarks", columnDefinition = "TEXT")
    private String approval_remarks;
}
