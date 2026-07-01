package com.thinkerscave.admission.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Counseling note recorded during an inquiry/admission consultation.
 * Org scope is inherited through the parent Inquiry.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "counseling_note",
        indexes = {
                @Index(name = "idx_cn_inquiry", columnList = "inquiry_id")
        }
)
public class CounselingNote extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    @EqualsAndHashCode.Include
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(name = "student_requirements", columnDefinition = "TEXT")
    private String studentRequirements;

    @Column(name = "parent_concerns", columnDefinition = "TEXT")
    private String parentConcerns;

    @Column(name = "campus_visit_info", columnDefinition = "TEXT")
    private String campusVisitInfo;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
