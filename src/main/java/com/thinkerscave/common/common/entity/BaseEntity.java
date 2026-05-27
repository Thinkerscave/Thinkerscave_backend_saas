package com.thinkerscave.common.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Root base entity for all new domain entities.
 *
 * <p>Provides:
 * <ul>
 *   <li>Auto-generated numeric primary key ({@code id})</li>
 *   <li>JPA optimistic locking ({@code version})</li>
 * </ul>
 *
 * <p>Most new entities should extend {@link AuditableBaseEntity} (or
 * {@link OrganizationScopedEntity} for tenant-aware entities) instead of
 * extending this class directly.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
