package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fixed_activity_group")
public class FixedActivityGroup extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixed_activity_group_id")
    @EqualsAndHashCode.Include
    private Long fixedActivityGroupId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixed_activity_id", nullable = false)
    private FixedActivity fixedActivity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_group_id", nullable = false)
    private AcademicGroup academicGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private AcademicSection section;

    public boolean appliesTo(Long groupId, Long sectionId) {
        if (!academicGroup.getAcademicGroupId().equals(groupId)) return false;
        return section == null || (sectionId != null && section.getSectionId().equals(sectionId));
    }
}
