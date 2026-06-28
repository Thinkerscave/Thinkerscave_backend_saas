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
        name = "features",
        indexes = {
                @Index(name = "idx_feature_code", columnList = "feature_code"),
                @Index(name = "idx_feature_name", columnList = "feature_name"),
                @Index(name = "idx_feature_module", columnList = "module"),
                @Index(name = "idx_feature_category", columnList = "category")
        }
)
public class Feature extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Unique Feature Code.
     * Example : FEATURE001
     */
    @Column(name = "feature_code", nullable = false, unique = true, length = 50)
    private String featureCode;

    /**
     * Feature Name.
     */
    @Column(name = "feature_name", nullable = false, length = 150)
    private String featureName;

    /**
     * Short Display Name.
     */
    @Column(name = "display_name", length = 150)
    private String displayName;

    /**
     * Module Name.
     * Example:
     * Student
     * Staff
     * Attendance
     * Finance
     */
    @Column(name = "module", nullable = false, length = 100)
    private String module;

    /**
     * Category.
     * Example:
     * CORE
     * ACADEMIC
     * ADMINISTRATION
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * Parent Feature.
     * Useful for grouping.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_feature_id")
    private Feature parentFeature;

    /**
     * API Key / Internal Identifier.
     * Example:
     * STUDENT_MODULE
     */
    @Column(name = "feature_key", nullable = false, unique = true, length = 100)
    private String featureKey;

    /**
     * Description.
     */
    @Column(name = "description", length = 2000)
    private String description;

    /**
     * Icon.
     */
    @Column(name = "icon", length = 100)
    private String icon;

    /**
     * Display Order.
     */
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    /**
     * Premium Feature.
     */
    @Builder.Default
    @Column(name = "premium_feature")
    private Boolean premiumFeature = false;

    /**
     * Visible in Catalog.
     */
    @Builder.Default
    @Column(name = "visible")
    private Boolean visible = true;

    /**
     * Default Enabled.
     */
    @Builder.Default
    @Column(name = "default_enabled")
    private Boolean defaultEnabled = false;

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