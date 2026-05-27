package com.thinkerscave.common.promotion.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Per-student decision row inside a {@link PromotionBatch}. Closes the
 * {@code fromEnrollmentId} and (for promoted/graduated) seeds the
 * {@code toEnrollmentId} in the next academic year.
 */
@Entity
@Table(name = "promotion_record",
        indexes = {
                @Index(name = "idx_promo_record_batch", columnList = "promotion_batch_id"),
                @Index(name = "idx_promo_record_student", columnList = "student_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRecord extends AuditableBaseEntity {

    @Column(name = "promotion_batch_id", nullable = false)
    private Long promotionBatchId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "from_enrollment_id", nullable = false)
    private Long fromEnrollmentId;

    @Column(name = "to_enrollment_id")
    private Long toEnrollmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 24)
    private PromotionDecision decision;

    @Column(name = "reason", length = 500)
    private String reason;
}
