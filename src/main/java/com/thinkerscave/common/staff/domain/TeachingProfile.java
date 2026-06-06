package com.thinkerscave.common.staff.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff_teaching_profile", indexes = {
        @Index(name = "idx_teaching_profile_org", columnList = "organization_id"),
        @Index(name = "idx_teaching_profile_staff", columnList = "staff_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeachingProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "staff_id", nullable = false, unique = true)
    private Long staffId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "subjects_can_teach", length = 500)
    private String subjectsCanTeach;

    @Column(name = "preferred_subjects", length = 500)
    private String preferredSubjects;

    @Column(name = "teaching_levels", length = 200)
    private String teachingLevels;

    @Column(name = "can_substitute_for", length = 500)
    private String canSubstituteFor;

    @Column(name = "cannot_substitute_for", length = 500)
    private String cannotSubstituteFor;

    @Column(name = "qualification", length = 200)
    private String qualification;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
