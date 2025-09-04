package com.thinkerscave.common.student.domain;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "section" ,schema = "public")
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "section_name", nullable = false)
    private String sectionName;

    @ManyToOne
    @JoinColumn(name = "class_entity_class_id")
    private ClassEntity classEntity;
}