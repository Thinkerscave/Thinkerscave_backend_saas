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
        name = "academic_group",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_academic_group_year_code",
                        columnNames = {"academic_year_id", "code"}
                )
        },
        indexes = {
                @Index(name = "idx_academic_group_year_active", columnList = "academic_year_id, is_active"),
                @Index(name = "idx_academic_group_stage", columnList = "stage")
        }
)
public class AcademicGroup extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "academic_group_id")
    @EqualsAndHashCode.Include
    private Long academicGroupId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 40)
    private AcademicStage stage;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @PositiveOrZero
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getAcademicGroupName() {
        return name;
    }

    /** Cross-module compatibility alias during Academics rebuild. */
    public void setAcademicGroupName(String academicGroupName) {
        this.name = academicGroupName;
    }

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getAcademicGroupCode() {
        return code;
    }

    public Long getClassId() {
        return academicGroupId;
    }

    public String getClassName() {
        return name;
    }

    public String getClassCode() {
        return code;
    }

    /** Cross-module compatibility alias during Academics rebuild. */
    public void setAcademicGroupCode(String academicGroupCode) {
        this.code = academicGroupCode;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
