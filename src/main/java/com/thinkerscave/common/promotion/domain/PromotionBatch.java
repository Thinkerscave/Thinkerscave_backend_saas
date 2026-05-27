package com.thinkerscave.common.promotion.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Year-end mass promotion batch — orchestrates promoting students from the
 * "from" academic year/class into the "to" academic year/class. Detailed
 * per-student decisions live in {@link PromotionRecord}.
 */
@Entity
@Table(name = "promotion_batch",
        indexes = {
                @Index(name = "idx_promo_batch_org", columnList = "organization_id"),
                @Index(name = "idx_promo_batch_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionBatch extends OrganizationScopedEntity {

    @Column(name = "batch_code", nullable = false, length = 32)
    private String batchCode;

    @Column(name = "from_academic_year_id", nullable = false)
    private Long fromAcademicYearId;

    @Column(name = "to_academic_year_id", nullable = false)
    private Long toAcademicYearId;

    @Column(name = "from_class_id")
    private Long fromClassId;

    @Column(name = "to_class_id")
    private Long toClassId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PromotionStatus status;

    @Column(name = "planned_count")
    private Integer plannedCount;

    @Column(name = "processed_count")
    private Integer processedCount;

    @Column(name = "executed_on")
    private LocalDate executedOn;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
