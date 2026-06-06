package com.thinkerscave.common.student.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "student_achievement", indexes = {
    @Index(name = "idx_student_achievement_org", columnList = "organization_id"),
    @Index(name = "idx_student_achievement_student", columnList = "student_id"),
    @Index(name = "idx_student_achievement_category", columnList = "category")
})
public class StudentAchievement extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "achievement_id")
    private Long achievementId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "category", length = 32, nullable = false)
    private String category;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "achievement_date")
    private LocalDate achievementDate;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "awarded_by", length = 200)
    private String awardedBy;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
}
