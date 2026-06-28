package com.thinkerscave.platform.entity;

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
        name = "provisioning_template_items",
        indexes = {
                @Index(name = "idx_pti_template", columnList = "template_id"),
                @Index(name = "idx_pti_type", columnList = "item_type"),
                @Index(name = "idx_pti_key", columnList = "item_key")
        }
)
public class ProvisioningTemplateItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Provisioning Template.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProvisioningTemplate template;

    /**
     * Item Type.
     * Example:
     * MODULE
     * ROLE
     * PERMISSION
     * CLASS
     * SECTION
     * DEPARTMENT
     * DESIGNATION
     * CONFIGURATION
     */
    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    /**
     * Unique Item Key.
     */
    @Column(name = "item_key", nullable = false, length = 150)
    private String itemKey;

    /**
     * Display Name.
     */
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    /**
     * Item Value.
     */
    @Column(name = "item_value", length = 1000)
    private String itemValue;

    /**
     * JSON Configuration.
     */
    @Lob
    @Column(name = "configuration_json")
    private String configurationJson;

    /**
     * Mandatory Item.
     */
    @Builder.Default
    @Column(name = "mandatory")
    private Boolean mandatory = false;

    /**
     * Default Enabled.
     */
    @Builder.Default
    @Column(name = "enabled")
    private Boolean enabled = true;

    /**
     * Display Order.
     */
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    /**
     * Active Flag.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    /**
     * Internal Remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

}