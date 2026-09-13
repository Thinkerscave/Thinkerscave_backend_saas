package com.thinkerscave.admission.entity;

import com.thinkerscave.admission.enums.FollowUpType;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.enums.InquiryStatusConverter;
import com.thinkerscave.admission.enums.LeadSource;
import com.thinkerscave.admission.entity.converter.LeadSourceAttributeConverter;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Prospective student inquiry — one record per lead/prospect.
 * <p>
 * Multi-tenant isolation handled by schema context (schema-per-tenant architecture).
 * Status workflow drives the admission funnel from NEW → CONVERTED or LOST.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "inquiry",
        indexes = {
                @Index(name = "idx_inq_status", columnList = "status"),
                @Index(name = "idx_inq_mobile", columnList = "mobile_number"),
                @Index(name = "idx_inq_counselor", columnList = "assigned_counselor_id")
        }
)
public class Inquiry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    @EqualsAndHashCode.Include
    private Long inquiryId;

    // ─── Prospect Details ──────────────────────────────────────────────────

    @Column(name = "name", nullable = false, length = 100)
    private String name;

        @Column(name = "student_name", nullable = false, length = 100)
        private String studentName;

        @Column(name = "parent_contact_name", nullable = false, length = 100)
        private String parentContactName;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "inquiry_number", length = 40)
    private String inquiryNumber;

    @Column(name = "class_interested_in", nullable = false, length = 50)
    private String classInterestedIn;

    @Column(name = "academic_year_id")
    private Long academicYearId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Convert(converter = LeadSourceAttributeConverter.class)
    @Column(name = "inquiry_source", nullable = false, length = 30)
    private LeadSource inquirySource;

    @Column(name = "referred_by", length = 100)
    private String referredBy;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    // ─── CRM ──────────────────────────────────────────────────────────────

    @Column(name = "assigned_counselor_id")
    private Long assignedCounselorId;

    @Convert(converter = InquiryStatusConverter.class)
    @Column(name = "status", nullable = false, length = 30)
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    @Column(name = "last_follow_up_date")
    private LocalDateTime lastFollowUpDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_follow_up_type", length = 20)
    private FollowUpType lastFollowUpType;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    // ─── Progressive enrichment (filled in later via Lead 360, not at creation) ─────────

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    /** Student's current class today, distinct from classInterestedIn (the class being applied for). */
    @Column(name = "current_class", length = 50)
    private String currentClass;

    @Column(name = "previous_school", length = 150)
    private String previousSchool;

    @Column(name = "alternate_mobile_number", length = 20)
    private String alternateMobileNumber;

    /** e.g. Father / Mother / Guardian. */
    @Column(name = "contact_relationship", length = 30)
    private String contactRelationship;

    @Column(name = "campus_preference", length = 100)
    private String campusPreference;

    /** Free-form tri-state text: e.g. YES / NO / NOT_SURE. */
    @Column(name = "transport_required", length = 20)
    private String transportRequired;

    @Column(name = "hostel_required", length = 20)
    private String hostelRequired;

    @Column(name = "other_requirements", columnDefinition = "TEXT")
    private String otherRequirements;
}
