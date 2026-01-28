package com.thinkerscave.common.admission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import com.thinkerscave.common.admission.enums.InquiryStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.thinkerscave.common.auditing.Auditable;

@Entity
@Table(
    name = "inquiry",
    indexes = {
        @Index(name = "idx_inquiry_mobile", columnList = "mobile_number"),
        @Index(name = "idx_inquiry_class", columnList = "class_interested_in"),
        @Index(name = "idx_inquiry_source", columnList = "inquiry_source"),
        @Index(name = "idx_inquiry_counselor", columnList = "assigned_counselor_id"),
        @Index(name = "idx_inquiry_active", columnList = "is_deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long inquiryId;

    // --------------------
    // Student Details
    // --------------------

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "class_interested_in", nullable = false, length = 50)
    private String classInterestedIn;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    // --------------------
    // Enquiry Metadata
    // --------------------

    @Column(name = "inquiry_source", nullable = false, length = 30)
    private String inquirySource; // WEBSITE, SOCIAL_MEDIA, WALK_IN, PHONE

    @Column(name = "referred_by", length = 100)
    private String referredBy;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    // --------------------
    // Assignment & Status
    // --------------------

    @Column(name = "assigned_counselor_id")
    private Long assignedCounselorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InquiryStatus status; // NEW, CONTACTED, FOLLOW_UP_REQUIRED, READY_FOR_ADMISSION, CONVERTED, LOST, CLOSED

    // --------------------
    // Soft Delete
    // --------------------

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "last_follow_up_date")
    private LocalDateTime lastFollowUpDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_follow_up_type")
    private com.thinkerscave.common.admission.enums.FollowUpType lastFollowUpType;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    // --------------------
    // Lifecycle Hooks
    // --------------------

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = InquiryStatus.NEW;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }
}

