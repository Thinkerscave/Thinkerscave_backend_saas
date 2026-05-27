package com.thinkerscave.common.rbac.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.common.enums.GenericStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Responsibility — a third-tier authorization concept that sits beside
 * Role + Privilege (defined in {@code menum/}).
 *
 * <p>While Role is broad ("Teacher", "Admin") and Privilege is narrow
 * ("EDIT_STUDENT"), Responsibility expresses scoped extra duties such as
 * "Class Teacher of 5-A", "Exam Coordinator", "Fee Collection Officer".
 * Assigning a responsibility to a user grants the privileges in
 * {@link ResponsibilityPrivilege} on top of their base role privileges.
 */
@Entity
@Table(name = "responsibility",
        uniqueConstraints = @UniqueConstraint(name = "uk_responsibility_org_code",
                columnNames = {"organization_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Responsibility extends OrganizationScopedEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /** Optional scope hint, e.g. {@code CLASS}, {@code SECTION}, {@code SUBJECT}, {@code DEPARTMENT}. */
    @Column(name = "scope_type", length = 32)
    private String scopeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GenericStatus status;
}
