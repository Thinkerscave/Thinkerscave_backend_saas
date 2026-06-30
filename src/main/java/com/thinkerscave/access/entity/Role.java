package com.thinkerscave.access.entity;

import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "roles",
        indexes = {
                @Index(name = "idx_role_code", columnList = "role_code"),
                @Index(name = "idx_role_name", columnList = "role_name"),
                @Index(name = "idx_role_type", columnList = "role_type")
        }
)
public class Role extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Internal unique role code.
     * Example:
     * ROLE_STAFF
     * ROLE_ADMIN
     */
    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private String roleCode;

    /**
     * Display name.
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 100)
    private String roleName;

    /**
     * Description.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Fixed system role.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 50)
    private RoleType roleType;

    /**
     * Dashboard identifier.
     * Example:
     * STAFF
     * STUDENT
     * ADMIN
     */
    @Column(name = "dashboard_code", length = 50)
    private String dashboardCode;

    /**
     * Whether this is a system role.
     */
    @Builder.Default
    @Column(name = "system_role", nullable = false)
    private Boolean systemRole = true;

    /**
     * Active status.
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Display order.
     */
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 1;

    /**
     * Users assigned to this role.
     */
    @Builder.Default
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * Permissions assigned to this role.
     */
    @Builder.Default
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new HashSet<>();

}