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
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "user_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_permission",
                        columnNames = {
                                "user_id",
                                "menu_id"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_user_permission_user", columnList = "user_id"),
                @Index(name = "idx_user_permission_menu", columnList = "menu_id")
        }
)
public class UserPermission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Menu
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    /**
     * Override Permissions
     */
    @Builder.Default
    @Column(name = "can_view")
    private Boolean canView = false;

    @Builder.Default
    @Column(name = "can_manage")
    private Boolean canManage = false;

    @Builder.Default
    @Column(name = "can_approve")
    private Boolean canApprove = false;

    /**
     * Override enabled.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

}