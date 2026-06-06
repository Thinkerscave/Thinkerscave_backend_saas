package com.thinkerscave.common.admission.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Counseling notes captured on an inquiry. Multiple notes per inquiry are allowed
 * so we can preserve a chronological counselor narrative.
 */
@Entity
@Table(name = "counseling_note", indexes = {
        @Index(name = "idx_counseling_note_inquiry", columnList = "inquiry_id"),
        @Index(name = "idx_counseling_note_org", columnList = "organization_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CounselingNote extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "organization_id")
    private Long organizationId;
}
