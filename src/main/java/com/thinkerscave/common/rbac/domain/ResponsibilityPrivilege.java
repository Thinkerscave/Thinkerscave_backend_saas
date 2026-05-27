package com.thinkerscave.common.rbac.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Many-to-many join: which privileges (from {@code menum.privilege_master})
 * are granted by which {@link Responsibility}.
 */
@Entity
@Table(name = "responsibility_privilege",
        uniqueConstraints = @UniqueConstraint(name = "uk_resp_priv_resp_priv",
                columnNames = {"responsibility_id", "privilege_id"}),
        indexes = @Index(name = "idx_resp_priv_privilege", columnList = "privilege_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponsibilityPrivilege extends AuditableBaseEntity {

    @Column(name = "responsibility_id", nullable = false)
    private Long responsibilityId;

    @Column(name = "privilege_id", nullable = false)
    private Long privilegeId;
}
