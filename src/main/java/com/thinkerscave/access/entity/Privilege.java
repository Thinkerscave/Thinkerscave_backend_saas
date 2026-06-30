package com.thinkerscave.access.entity;

import com.thinkerscave.access.enums.PrivilegeType;
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
        name = "privileges",
        indexes = {
                @Index(name = "idx_privilege_code", columnList = "privilege_code"),
                @Index(name = "idx_privilege_name", columnList = "privilege_name"),
                @Index(name = "idx_privilege_type", columnList = "privilege_type")
        }
)
public class Privilege extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Internal unique privilege code.
     * Example:
     * VIEW
     * MANAGE
     * APPROVE
     */
    @Column(name = "privilege_code", nullable = false, unique = true, length = 50)
    private String privilegeCode;

    /**
     * Display name.
     */
    @Column(name = "privilege_name", nullable = false, unique = true, length = 100)
    private String privilegeName;

    /**
     * Description.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Privilege type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "privilege_type", nullable = false, unique = true, length = 30)
    private PrivilegeType privilegeType;

    /**
     * Display order.
     */
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 1;

    /**
     * Active status.
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Role Permission Mapping.
     */
    @Builder.Default
    @OneToMany(mappedBy = "privilege", fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    /**
     * User Permission Mapping.
     */
    @Builder.Default
    @OneToMany(mappedBy = "privilege", fetch = FetchType.LAZY)
    private Set<UserPermission> userPermissions = new HashSet<>();

}