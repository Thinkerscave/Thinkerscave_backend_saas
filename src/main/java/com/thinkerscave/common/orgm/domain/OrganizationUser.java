package com.thinkerscave.common.orgm.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Junction entity mapping users to organizations with specific roles.
 * Stored in TENANT SCHEMA (not public).
 * 
 * Example:
 * - User "Mr. Sharma" in organization "GITA College" with role "PRINCIPAL"
 * - User "Ms. Patel" in organization "GIFT College" with role "TEACHER"
 * 
 * @author System
 */
@Entity
@Table(name = "organization_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to organisation.id in PUBLIC schema.
     * Not a foreign key because organisations table is in different schema.
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Reference to user.id in THIS tenant schema.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Role name within this organization.
     * Examples: PRINCIPAL, TEACHER, ADMIN, ACCOUNTANT, etc.
     */
    @Column(name = "role_name", length = 50)
    private String roleName;

    /**
     * Whether the user's membership in this organization is active.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * When the user joined this organization.
     */
    @Column(name = "joined_at", updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    /**
     * Last updated timestamp (auto-updated by trigger).
     */
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
