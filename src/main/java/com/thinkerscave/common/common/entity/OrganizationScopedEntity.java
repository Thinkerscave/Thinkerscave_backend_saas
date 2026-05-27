package com.thinkerscave.common.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base entity for all tenant- / organization-scoped domain entities.
 *
 * <p>Adds:
 * <ul>
 *   <li>{@code organizationId} — soft tenant scoping at the row level (in
 *       addition to schema-per-tenant isolation)</li>
 *   <li>{@code deleted} flag — enables soft delete semantics (use
 *       {@code @SQLDelete} + {@code @Where(clause = "deleted = false")} on the
 *       concrete entity to enforce filtering)</li>
 * </ul>
 *
 * <p>Use this for any entity that conceptually "belongs to" an organization:
 * students, staff, fees, exams, attendance, etc.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class OrganizationScopedEntity extends AuditableBaseEntity {

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
