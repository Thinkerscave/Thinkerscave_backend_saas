package com.thinkerscave.admission.entity;

import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Formal application for admission, submitted after initial inquiry.
 * Links to the originating Inquiry where applicable.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "application_admission",
        indexes = {
                @Index(name = "idx_app_org_status", columnList = "organization_id, status"),
                @Index(name = "idx_app_inquiry", columnList = "inquiry_id"),
                @Index(name = "idx_app_number", columnList = "application_number", unique = true)
        }
)
public class ApplicationAdmission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    @EqualsAndHashCode.Include
    private Long applicationId;

    @Column(name = "application_number", unique = true, nullable = false, length = 30)
    private String applicationNumber;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** Optional link back to the originating inquiry */
    @Column(name = "inquiry_id")
    private Long inquiryId;

    // ─── Applicant ────────────────────────────────────────────────────────

    @Column(name = "applicant_name", nullable = false, length = 200)
    private String applicantName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "applying_for_class", length = 50)
    private String applyingForClass;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    // ─── Parent / Guardian ─────────────────────────────────────────────────

    @Column(name = "parent_name", length = 200)
    private String parentName;

    @Column(name = "parent_contact", length = 20)
    private String parentContact;

    @Column(name = "parent_email", length = 150)
    private String parentEmail;

    // ─── Status ────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_on")
    private LocalDate reviewedOn;

    @Column(name = "internal_comments", columnDefinition = "TEXT")
    private String internalComments;

    // ─── Documents (stored as file paths / URLs) ───────────────────────────

    @ElementCollection
    @CollectionTable(name = "application_documents",
            joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "document_url", length = 500)
    private List<String> uploadedDocuments;
}
