package com.thinkerscave.access.entity;

import com.thinkerscave.platform.entity.Organization;
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
        name = "organization_modules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_org_module",
                        columnNames = {
                                "organization_id",
                                "menu_id"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_org_module_org", columnList = "organization_id"),
                @Index(name = "idx_org_module_menu", columnList = "menu_id")
        }
)
public class OrganizationModule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Organization
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Module Menu
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    /**
     * Module Enabled
     */
    @Builder.Default
    @Column(name = "enabled")
    private Boolean enabled = true;

}