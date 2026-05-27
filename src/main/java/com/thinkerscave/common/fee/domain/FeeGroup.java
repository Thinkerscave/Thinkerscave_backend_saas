package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.common.enums.GenericStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Logical grouping of fee heads — e.g. {@code CORE_FEES}, {@code OPTIONAL_FEES},
 * {@code TRANSPORT_PACKAGES}. Used for reporting and UI grouping.
 */
@Entity
@Table(name = "fee_group",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_group_org_code",
                columnNames = {"organization_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeGroup extends OrganizationScopedEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GenericStatus status;
}
