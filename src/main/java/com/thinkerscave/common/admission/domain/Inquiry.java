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
public class Inquiry {

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

    @Column(name = "status", nullable = false, length = 30)
    private String status; // NEW, FOLLOW_UP, ADMITTED, LOST

    // --------------------
    // Audit Fields
    // --------------------

    @Column(name = "created_by", nullable = false, length = 30)
    private String createdBy; // SYSTEM / STAFF

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --------------------
    // Soft Delete
    // --------------------

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    // --------------------
    // Lifecycle Hooks
    // --------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "NEW";
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

