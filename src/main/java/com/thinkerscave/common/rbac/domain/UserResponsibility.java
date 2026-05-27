package com.thinkerscave.common.rbac.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Assignment of a {@link Responsibility} to a user.
 *
 * <p>{@code scopeRefId} holds the target id for scoped responsibilities
 * (e.g. class id for "Class Teacher of 5-A"); {@code null} for global
 * responsibilities.
 */
@Entity
@Table(name = "user_responsibility",
        indexes = {
                @Index(name = "idx_user_resp_user", columnList = "user_id"),
                @Index(name = "idx_user_resp_resp", columnList = "responsibility_id"),
                @Index(name = "idx_user_resp_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponsibility extends OrganizationScopedEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "responsibility_id", nullable = false)
    private Long responsibilityId;

    /** Optional scope target — e.g. class id, section id, department id. */
    @Column(name = "scope_ref_id")
    private Long scopeRefId;

    @Column(name = "academic_year_id")
    private Long academicYearId;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
