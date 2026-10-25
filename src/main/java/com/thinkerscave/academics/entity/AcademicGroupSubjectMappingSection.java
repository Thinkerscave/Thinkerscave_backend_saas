package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academic_group_subject_mapping_section",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_subject_mapping_section",
                columnNames = {"academic_group_subject_mapping_id", "section_id"}
        )
)
public class AcademicGroupSubjectMappingSection extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "academic_group_subject_mapping_section_id")
    @EqualsAndHashCode.Include
    private Long academicGroupSubjectMappingSectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_group_subject_mapping_id", nullable = false)
    private AcademicGroupSubjectMapping mapping;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private AcademicSection section;
}