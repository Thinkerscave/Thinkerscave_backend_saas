package com.thinkerscave.student.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "student_achievement")
public class StudentAchievement extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "achievement_id")
    private Long achievementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "achievement_date")
    private LocalDate achievementDate;

    @Column(name = "issuer", length = 255)
    private String issuer;

    @Column(name = "rank_position", length = 50)
    private String rankPosition;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "certificate_document_id")
    private Long certificateDocumentId;

    @Column(name = "active")
    private Boolean active = true;
}
