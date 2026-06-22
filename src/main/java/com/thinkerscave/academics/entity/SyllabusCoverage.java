package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.CoverageStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "syllabus_coverage",
        indexes = {
                @Index(name = "idx_coverage_status", columnList = "status"),
                @Index(name = "idx_coverage_teacher", columnList = "teacher_id")
        }
)
public class SyllabusCoverage extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coverage_id")
    @EqualsAndHashCode.Include
    private Long coverageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CoverageStatus status = CoverageStatus.NOT_STARTED;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}