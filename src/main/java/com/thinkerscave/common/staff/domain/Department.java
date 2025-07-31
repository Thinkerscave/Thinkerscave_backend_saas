package com.thinkerscave.common.staff.domain;


import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "department")
public class Department extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long id;

    @Column(name = "department_name", nullable = false, unique = true, length = 255)
    private String departmentName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "department_code", unique = true, nullable = false)
    private String departmentCode;

    @Column(name = "is_active")
    private Boolean isActive = true;
}

