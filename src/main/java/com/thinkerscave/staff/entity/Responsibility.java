package com.thinkerscave.staff.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "responsibility",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_responsibility_code",
                        columnNames = "responsibility_code"
                )
        },
        indexes = {
                @Index(name = "idx_responsibility_code", columnList = "responsibility_code"),
                @Index(name = "idx_responsibility_active", columnList = "active")
        }
)
public class Responsibility extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "responsibility_id")
    @EqualsAndHashCode.Include
    private Long responsibilityId;

    @NotBlank
    @Size(max = 30)
    @Column(name = "responsibility_code", nullable = false, length = 30)
    private String responsibilityCode;

    @NotBlank
    @Size(max = 150)
    @Column(name = "responsibility_name", nullable = false, length = 150)
    private String responsibilityName;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "system_defined", nullable = false)
    private Boolean systemDefined = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}