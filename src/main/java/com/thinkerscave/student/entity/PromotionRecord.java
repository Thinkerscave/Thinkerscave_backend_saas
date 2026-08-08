package com.thinkerscave.student.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.student.enums.PromotionDecision;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "promotion_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_promotion_batch_student", columnNames = {"batch_id", "student_id"})
        }
)
public class PromotionRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "record_id")
    private Long recordId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private PromotionBatch batch;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "from_enrollment_id")
    private Long fromEnrollmentId;

    @Column(name = "to_enrollment_id")
    private Long toEnrollmentId;

    @Column(name = "from_class_id")
    private Long fromClassId;

    @Column(name = "to_class_id")
    private Long toClassId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    private PromotionDecision decision = PromotionDecision.PROMOTED;

    @Column(name = "reason", length = 500)
    private String reason;
}
