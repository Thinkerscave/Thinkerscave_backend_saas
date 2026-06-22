package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.AcademicStage;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academic_class",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_class_year_code",
                        columnNames = {
                                "academic_year_id",
                                "class_code"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_class_code", columnList = "class_code"),
                @Index(name = "idx_class_stage", columnList = "academic_stage")
        }
)
public class AcademicClass extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    @EqualsAndHashCode.Include
    private Long classId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotBlank
    @Size(max = 30)
    @Column(name = "class_code", nullable = false, length = 30)
    private String classCode;

    @NotBlank
    @Size(max = 100)
    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "academic_stage", nullable = false, length = 30)
    private AcademicStage academicStage;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}