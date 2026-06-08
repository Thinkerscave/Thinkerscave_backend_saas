package com.thinkerscave.access.entity;

import com.thinkerscave.shared.entity.Auditable;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true,
        callSuper = false
)
@Table(
        name = "role",
        indexes = {
                @Index(name = "idx_role_code", columnList = "role_code"),
                @Index(name = "idx_role_active", columnList = "active")
        }
)
public class Role extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "role_id")
    private Long roleId;

    /**
     * Example:
     * ROLE_TEACHER
     * ROLE_PARENT
     * ROLE_ADMIN
     */
    @Column(name = "role_code",
            nullable = false,
            unique = true,
            length = 100)
    private String roleCode;

    /**
     * Display Name
     */
    @Column(name = "role_name",
            nullable = false,
            length = 100)
    private String roleName;

    /**
     * Description
     */
    @Column(name = "description",
            columnDefinition = "TEXT")
    private String description;

    /**
     * System roles cannot be deleted.
     */
    @Builder.Default
    @Column(name = "system_role")
    private Boolean systemRole = false;

    /**
     * Active flag.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;
}