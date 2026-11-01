package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
        name = "academic_section",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_academic_section_group_code",
                        columnNames = {"academic_group_id", "code"}
                )
        },
        indexes = {
                @Index(name = "idx_academic_section_group_active", columnList = "academic_group_id, is_active")
        }
)
public class AcademicSection extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    @EqualsAndHashCode.Include
    private Long sectionId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_group_id", nullable = false)
    private AcademicGroup academicGroup;

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Positive
    @Column(name = "capacity")
    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_resource_id")
    private AcademicResource defaultResource;

    @NotNull
    @PositiveOrZero
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getSectionName() {
        return name;
    }

    /** Cross-module compatibility alias during Academics rebuild. */
    public String getSectionCode() {
        return code;
    }

    public AcademicGroup getAcademicClass() {
        return academicGroup;
    }

    public void setAcademicClass(AcademicClass legacyClass) {
        if (legacyClass == null) {
            academicGroup = null;
            return;
        }
        AcademicGroup group = new AcademicGroup();
        group.setAcademicGroupId(legacyClass.getClassId());
        group.setAcademicYear(legacyClass.getAcademicYear());
        group.setName(legacyClass.getName());
        group.setCode(legacyClass.getCode());
        group.setStage(legacyClass.getStage());
        academicGroup = group;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
