package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 👁️ SyllabusAccess Entity - The Curriculum Engagement Audit Log
 * 
 * 🏛️ Business Purpose:
 * This entity serves as the high-fidelity audit trail for any interaction with
 * a published syllabus. It helps administrators understand student engagement
 * levels and ensures that sensitive institutional curriculum data is being
 * accessed by authorized users. It solves the "Engagement Visibility" problem
 * by tracking view counts and session times.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Security Admins**: Monitor access logs to detect unusual patterns or
 * potential data scraping.
 * - **Teachers / Tutors**: Analyze which syllabus versions are being viewed
 * most
 * frequently to identify high-interest curriculum areas.
 * - **Students / Parents**: Indirectly benefit as the system can suggest
 * materials based on their viewing history.
 * 
 * 🔄 Academic Flow Position:
 * Situated in the **Monitoring Phase**. It runs silently in the background
 * whenever a user opens the curriculum dashboard.
 * 
 * 🏗️ Design Intent:
 * Built as a "Passive Audit" structure. It uses an upsert strategy to maintain
 * a single record per user-syllabus pair while incrementing engagement metrics
 * (view counts/time spent).
 */
@Entity
@Table(name = "syllabus_access")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SyllabusAccess extends Auditable {

    /**
     * Primary stable identifier for the access record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Long accessId;

    /**
     * The user (Student, Parent, or Staff) who interacted with the curriculum.
     * Hierarchy: Essential for per-user engagement reporting.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The target syllabus version that was accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    private Syllabus syllabus;

    /**
     * Timestamp of the very first time this user opened this specific syllabus.
     */
    @Column(name = "first_accessed_date")
    private LocalDateTime firstAccessedDate;

    /**
     * Timestamp of the most recent interaction.
     * Why: Used to show "Recently Viewed" lists in the user dashboard.
     */
    @Column(name = "last_accessed_date")
    private LocalDateTime lastAccessedDate;

    /**
     * Internal counter of how many times the user has opened this material.
     * Analytics: High view counts on specific sections can indicate content
     * complexity.
     */
    @Column(name = "view_count")
    private Integer viewCount = 0;

    /**
     * Total estimated minutes the user spent viewing the curriculum.
     */
    @Column(name = "total_time_spent_minutes")
    private Integer totalTimeSpentMinutes;

    /**
     * Classification of the user during this session (STUDENT, PARENT, TEACHER).
     * Why: Helps filter analytics by persona.
     */
    @Column(name = "access_type", length = 50)
    private String accessType;

    /**
     * The platform used (WEB, MOBILE, TABLET).
     * Technical Rationale: Helps the product team optimize UI delivery based on
     * device usage.
     */
    @Column(name = "device_type", length = 50)
    private String deviceType;

    /**
     * The most recent operation performed (e.g., "PRINTED", "VIEWED",
     * "DOWNLOADED").
     * Compliance: Provides proof of document delivery to the stakeholder.
     */
    @Column(name = "last_action", length = 255)
    private String lastAction;
}
