package com.thinkerscave.admission.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "lead_counselor_assignment",
        indexes = {
                @Index(name = "idx_lca_inquiry", columnList = "inquiry_id"),
                @Index(name = "idx_lca_active", columnList = "active"),
                @Index(name = "idx_lca_new_counselor", columnList = "new_counselor_staff_id")
        })
public class LeadCounselorAssignment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    @EqualsAndHashCode.Include
    private Long assignmentId;

    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;

    @Column(name = "previous_counselor_staff_id")
    private Long previousCounselorStaffId;

    @Column(name = "new_counselor_staff_id", nullable = false)
    private Long newCounselorStaffId;

    @Column(name = "reason", length = 300)
    private String reason;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Column(name = "assigned_by_username", length = 100)
    private String assignedByUsername;

    @Column(name = "assigned_on", nullable = false)
    private LocalDateTime assignedOn;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}