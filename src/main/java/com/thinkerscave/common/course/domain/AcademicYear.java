package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.orgm.domain.Organisation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 📅 AcademicYear Entity - The Temporal Framework of ThinkersCave
 * 
 * 🏛️ Business Purpose:
 * AcademicYear acts as the master timeline for all institutional operations.
 * Every Syllabus, Student Enrollment, and Grade record is indexed by an
 * Academic Year, enabling the platform to support year-over-year data analysis
 * and historical transitions. It solves the problem of "Data Overlap" by
 * partitioning school activity into distinct calendar blocks.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Finance Department**: Uses start/end dates to define the fiscal cycle for
 * tuition fee collection.
 * - **Registrar / Admissions**: Ensures students are enrolled in the correct
 * current intake cycle.
 * - **Teachers**: Reference the current year to see their active teaching
 * workload and syllabus.
 * - **IT Admins**: Manage the transition (Promotion) from one academic year to
 * the next.
 * 
 * 🔄 Academic Flow Position:
 * This is a **Global Filter Level**. Almost all business operations in
 * ThinkersCave (Attendance, Exams, Fees) require an active Academic Year
 * context before they can be executed.
 * 
 * 🏗️ Design Intent:
 * Built with a "Single Current Truth" philosophy—only one year is marked as
 * `isCurrent` for a specific organisation at any given time, serving as the
 * default context for the entire application.
 */
@Entity
@Table(name = "academic_years")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AcademicYear extends Auditable {

    /**
     * Primary stable internal identifier for the academic session.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "academic_year_id")
    private Long academicYearId;

    /**
     * Unique Internal Code (e.g., AY2024-25).
     * Why: This is used as a stable reference in database queries and
     * integrations with external government education systems.
     */
    @Column(name = "year_code", unique = true, nullable = false, length = 50)
    private String yearCode;

    /**
     * Human-friendly name displayed in UI selects (e.g., "Academic Year
     * 2024-2025").
     */
    @Column(name = "year_name", nullable = false, length = 100)
    private String yearName;

    /**
     * Physical start date of the academic session.
     * Business Logic: Used to validate when students can first start logging
     * progress.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Physical end date of the academic session.
     * Business Logic: Used to trigger end-of-year reports and closures.
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * The tenant owner of this calendar cycle.
     * Hierarchy: Strict institutional isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organisation organization;

    /**
     * Flag indicating if this is the "Live" year for the institution.
     * Business Rule: Only one record per organization should have this set to
     * true to prevent data entry confusion.
     */
    @Column(name = "is_current")
    private Boolean isCurrent = false;

    /**
     * Availability flag for archival.
     * Why: Historical years are never deleted; they are kept for alumni
     * records and compliance audits.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Optional space for notes regarding holidays, session changes, or
     * specific year-level events.
     */
    @Column(columnDefinition = "TEXT")
    private String description;
}
