package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Resolved timetable period for one fixed activity on an exact group/section/day
 * combination. The period owns start/end time, so fixed activities never store
 * arbitrary time values independently.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "fixed_activity_period",
        indexes = {
                @Index(name = "idx_fixed_activity_period_period", columnList = "timetable_period_id")
        }
)
public class FixedActivityPeriod extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixed_activity_period_id")
    @EqualsAndHashCode.Include
    private Long fixedActivityPeriodId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixed_activity_id", nullable = false)
    private FixedActivity fixedActivity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_group_id", nullable = false)
    @EqualsAndHashCode.Include
    private AcademicGroup academicGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    @EqualsAndHashCode.Include
    private AcademicSection section;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    @EqualsAndHashCode.Include
    private DayOfWeek dayOfWeek;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_period_id", nullable = false)
    private TimetablePeriod timetablePeriod;

    public boolean appliesTo(Long groupId, Long sectionId) {
        if (!academicGroup.getAcademicGroupId().equals(groupId)) return false;
        return section == null || (sectionId != null && section.getSectionId().equals(sectionId));
    }
}
