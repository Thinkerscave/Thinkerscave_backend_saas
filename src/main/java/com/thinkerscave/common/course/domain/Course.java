package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.orgm.domain.Organisation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Course entity - represents educational programs
 * Examples: B.Tech, MBA, Grade 10, Class XII, etc.
 */
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Course extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_code", unique = true, nullable = false, length = 50)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_months")
    private Integer durationMonths; // Total duration in months

    @Column(name = "duration_years")
    private Integer durationYears; // For display (e.g., 4 years)

    @Column(name = "level", length = 50)
    private String level; // Undergraduate, Postgraduate, School, Diploma

    @Column(name = "degree_type", length = 100)
    private String degreeType; // B.Tech, MBA, M.Sc, High School, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organisation organization;

    @Column(name = "total_credits")
    private Integer totalCredits;

    @Column(name = "min_credits_required")
    private Integer minCreditsRequired;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSubjectMapping> subjectMappings = new ArrayList<>();

    @Column(name = "eligibility_criteria", columnDefinition = "TEXT")
    private String eligibilityCriteria;

    @Column(name = "fees")
    private Double fees;
}
