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
 * A follow-up interaction against an inquiry.
 * Org scope is inherited through the parent Inquiry.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "inquiry_follow_up",
        indexes = {
                @Index(name = "idx_fu_inquiry", columnList = "inquiry_id"),
                @Index(name = "idx_fu_date", columnList = "follow_up_date")
        }
)
public class InquiryFollowUp extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_up_id")
    @EqualsAndHashCode.Include
    private Long followUpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_type", nullable = false, length = 20)
    private FollowUpType followUpType;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after", length = 30)
    private InquiryStatus statusAfter;

    @Column(name = "follow_up_date", nullable = false)
    private LocalDateTime followUpDate;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;
}
