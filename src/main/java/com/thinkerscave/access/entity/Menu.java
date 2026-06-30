package com.thinkerscave.access.entity;

import com.thinkerscave.access.enums.MenuType;
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
        name = "menus",
        indexes = {
                @Index(name = "idx_menu_code", columnList = "menu_code"),
                @Index(name = "idx_menu_name", columnList = "menu_name"),
                @Index(name = "idx_menu_route", columnList = "route"),
                @Index(name = "idx_menu_parent", columnList = "parent_menu_id")
        }
)
public class Menu extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Internal menu code.
     * Example:
     * ADMISSION
     * STUDENTS
     * ATTENDANCE_REPORT
     */
    @Column(name = "menu_code", nullable = false, unique = true, length = 100)
    private String menuCode;

    /**
     * Display name.
     */
    @Column(name = "menu_name", nullable = false, length = 150)
    private String menuName;

    /**
     * Short description.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Angular route.
     */
    @Column(name = "route", length = 255)
    private String route;

    /**
     * Sidebar icon.
     * Example:
     * pi pi-users
     */
    @Column(name = "icon", length = 100)
    private String icon;

    /**
     * Module or Page.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type", nullable = false, length = 30)
    private MenuType menuType;

    /**
     * Parent menu.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_menu_id")
    private Menu parentMenu;

    /**
     * Child menus.
     */
    @Builder.Default
    @OneToMany(mappedBy = "parentMenu", fetch = FetchType.LAZY)
    private Set<Menu> childMenus = new HashSet<>();

    /**
     * Sidebar display order.
     */
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 1;

    /**
     * Visible in sidebar.
     */
    @Builder.Default
    @Column(name = "show_in_sidebar")
    private Boolean showInSidebar = true;

    /**
     * Active status.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    /**
     * Default landing page.
     */
    @Builder.Default
    @Column(name = "default_page")
    private Boolean defaultPage = false;

    /**
     * Role Permission Mapping.
     */
    @Builder.Default
    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    /**
     * User Permission Mapping.
     */
    @Builder.Default
    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY)
    private Set<UserPermission> userPermissions = new HashSet<>();

}