package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
                        name = "uk_class_section",
                        columnNames = {
                                "class_id",
                                "section_name"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_section_name", columnList = "section_name")
        }
)
public class AcademicSection extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    @EqualsAndHashCode.Include
    private Long sectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private AcademicClass academicClass;

    @NotBlank
    @Size(max = 20)
    @Column(name = "section_name", nullable = false, length = 20)
    private String sectionName;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}