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
        name = "provisioning_templates",
        indexes = {
                @Index(name = "idx_template_code", columnList = "template_code"),
                @Index(name = "idx_template_name", columnList = "template_name"),
                @Index(name = "idx_template_type", columnList = "institution_type")
        }
)
public class ProvisioningTemplate extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Business Template Code.
     * Example:
     * CBSE_SCHOOL
     * COLLEGE
     * COACHING
     */
    @Column(name = "template_code", nullable = false, unique = true, length = 50)
    private String templateCode;

    /**
     * Template Name.
     */
    @Column(name = "template_name", nullable = false, unique = true, length = 150)
    private String templateName;

    /**
     * Institution Type.
     */
    @Column(name = "institution_type", nullable = false, length = 100)
    private String institutionType;

    /**
     * Provisioning Template Version.
     */
    @Column(name = "template_version", nullable = false, length = 20)
    private String templateVersion;

    /**
     * Description.
     */
    @Column(name = "description", length = 2000)
    private String description;

    /**
     * Default Academic Structure.
     */
    @Builder.Default
    @Column(name = "academic_structure_enabled")
    private Boolean academicStructureEnabled = true;

    /**
     * Default Roles.
     */
    @Builder.Default
    @Column(name = "roles_enabled")
    private Boolean rolesEnabled = true;

    /**
     * Default Permissions.
     */
    @Builder.Default
    @Column(name = "permissions_enabled")
    private Boolean permissionsEnabled = true;

    /**
     * Default Classes.
     */
    @Builder.Default
    @Column(name = "classes_enabled")
    private Boolean classesEnabled = true;

    /**
     * Default Sections.
     */
    @Builder.Default
    @Column(name = "sections_enabled")
    private Boolean sectionsEnabled = true;

    /**
     * Default Departments.
     */
    @Builder.Default
    @Column(name = "departments_enabled")
    private Boolean departmentsEnabled = true;

    /**
     * Default Designations.
     */
    @Builder.Default
    @Column(name = "designations_enabled")
    private Boolean designationsEnabled = true;

    /**
     * Seed Master Data.
     */
    @Builder.Default
    @Column(name = "seed_master_data")
    private Boolean seedMasterData = true;

    /**
     * Active.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    /**
     * Remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

}