package com.thinkerscave.admission.entity;

import com.thinkerscave.admission.enums.FollowUpType;
import com.thinkerscave.admission.enums.InquiryStatus;
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

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "class_interested_in", nullable = false, length = 50)
    private String classInterestedIn;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "inquiry_source", length = 50)
    private String inquirySource;

    @Column(name = "referred_by", length = 100)
    private String referredBy;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    // ─── CRM ──────────────────────────────────────────────────────────────

    @Column(name = "assigned_counselor_id")
    private Long assignedCounselorId;

    @Enumerated(EnumType.STRING)
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
}
