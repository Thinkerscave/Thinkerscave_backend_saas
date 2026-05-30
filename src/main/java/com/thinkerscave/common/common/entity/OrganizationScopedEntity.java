package com.thinkerscave.common.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

/**
 * Base entity for all tenant- / organization-scoped domain entities.
 *
 * <p>Adds:
 * <ul>
 *   <li>{@code organizationId} — soft tenant scoping at the row level (in
 *       addition to schema-per-tenant isolation)</li>
 *   <li>Automatic soft delete via Hibernate {@code @SoftDelete} — all
 *       {@code repository.delete()} calls are converted to
 *       {@code UPDATE ... SET deleted = true}, and all queries automatically
 *       filter out soft-deleted rows.</li>
 * </ul>
 *
 * <p>Use this for any entity that conceptually "belongs to" an organization:
 * students, staff, fees, exams, attendance, etc.
 */
@MappedSuperclass
@Getter
@Setter
@SoftDelete(columnName = "deleted")
public abstract class OrganizationScopedEntity extends AuditableBaseEntity {

    @Column(name = "organization_id")
    private Long organizationId;
}
