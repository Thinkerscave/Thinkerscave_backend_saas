package com.thinkerscave.access.entity;

import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.staff.entity.Responsibility;
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
        name = "responsibility_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_responsibility_permission",
                        columnNames = { "organization_id", "responsibility_id", "menu_id" }
                )
        },
        indexes = {
                @Index(name = "idx_resp_permission_resp", columnList = "responsibility_id"),
                @Index(name = "idx_resp_permission_menu", columnList = "menu_id")
        }
)
public class ResponsibilityPermission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsibility_id", nullable = false)
    private Responsibility responsibility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Builder.Default
    @Column(name = "can_view")
    private Boolean canView = false;

    @Builder.Default
    @Column(name = "can_manage")
    private Boolean canManage = false;

    @Builder.Default
    @Column(name = "can_approve")
    private Boolean canApprove = false;
}
