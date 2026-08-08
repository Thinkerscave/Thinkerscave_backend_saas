package com.thinkerscave.student.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.student.enums.PromotionBatchStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "promotion_batch")
public class PromotionBatch extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "batch_code", nullable = false, length = 50)
    private String batchCode;

    @Column(name = "from_academic_year_id", nullable = false)
    private Long fromAcademicYearId;

    @Column(name = "to_academic_year_id", nullable = false)
    private Long toAcademicYearId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PromotionBatchStatus status = PromotionBatchStatus.DRAFT;

    @Column(name = "planned_count")
    private Integer plannedCount = 0;

    @Column(name = "processed_count")
    private Integer processedCount = 0;

    @Column(name = "executed_on")
    private LocalDateTime executedOn;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
