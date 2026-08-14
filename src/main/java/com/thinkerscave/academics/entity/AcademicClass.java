package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.AcademicStage;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
                        name = "uk_academic_class_year_code",
                        columnNames = {"academic_year_id", "code"}
                )
        },
        indexes = {
                @Index(name = "idx_academic_class_year_active", columnList = "academic_year_id, is_active"),
                @Index(name = "idx_academic_class_stage", columnList = "stage")
        }
)
public class AcademicClass extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    @EqualsAndHashCode.Include
    private Long classId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 40)
    private AcademicStage stage;

    @NotNull
    @PositiveOrZero
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getClassName() {
        return name;
    }

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getClassCode() {
        return code;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
