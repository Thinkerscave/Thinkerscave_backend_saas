package com.thinkerscave.common.student.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "class" ,schema = "public")
public class ClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_name", nullable = false)
    private String className;
}