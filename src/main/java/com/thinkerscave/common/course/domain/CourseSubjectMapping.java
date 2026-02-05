package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * CourseSubjectMapping - Junction table
 * Maps which subjects belong to which course and in which semester
 */
@Entity
@Table(name = "course_subject_mapping", indexes = {
        @Index(name = "idx_csm_org", columnList = "organization_id")
})
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "organization_id = :tenantId")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CourseSubjectMapping extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.thinkerscave.common.orgm.domain.Organisation organization;

    @Column(name = "semester")
    private Integer semester; // Which semester this subject is taught

    @Column(name = "is_mandatory")
    private Boolean isMandatory = true; // Core vs Elective

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder; // For sorting subjects
}
