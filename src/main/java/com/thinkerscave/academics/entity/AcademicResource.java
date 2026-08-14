package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.ResourceType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academic_resource",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_academic_resource_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_academic_resource_type", columnList = "resource_type"),
                @Index(name = "idx_academic_resource_active", columnList = "is_active")
        }
)
public class AcademicResource extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "academic_resource_id")
    @EqualsAndHashCode.Include
    private Long academicResourceId;

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceType resourceType;

    @Positive
    @Column(name = "capacity")
    private Integer capacity;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
