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
@Table(
        name = "subject_choice_group_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subject_choice_group_member",
                columnNames = {"subject_choice_group_id", "subject_id"}
        ),
        indexes = @Index(name = "idx_subject_choice_group_member_subject", columnList = "subject_id")
)
public class SubjectChoiceGroupMember extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_choice_group_member_id")
    @EqualsAndHashCode.Include
    private Long subjectChoiceGroupMemberId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_choice_group_id", nullable = false)
    private SubjectChoiceGroup choiceGroup;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
}
