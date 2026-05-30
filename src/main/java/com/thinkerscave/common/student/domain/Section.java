package com.thinkerscave.common.student.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "section", indexes = {
    @Index(name = "idx_section_class", columnList = "class_entity_class_id"),
    @Index(name = "idx_section_org", columnList = "organization_id")
})
public class Section extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    @EqualsAndHashCode.Include
    private Long sectionId;

    @Column(name = "section_name", nullable = false)
    private String sectionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_entity_class_id")
    private ClassEntity classEntity;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}