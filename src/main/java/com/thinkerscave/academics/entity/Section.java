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
        name = "section",
        indexes = {
                @Index(name = "idx_section_code", columnList = "section_code")
        }
)
public class Section extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    @EqualsAndHashCode.Include
    private Long sectionId;

    @Column(name = "section_code", nullable = false, unique = true, length = 30)
    private String sectionCode;

    @Column(name = "section_name", nullable = false, length = 50)
    private String sectionName;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}