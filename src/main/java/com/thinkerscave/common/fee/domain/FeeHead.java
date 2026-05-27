package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.common.enums.GenericStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Master catalogue of fee heads — e.g. {@code TUITION}, {@code TRANSPORT},
 * {@code HOSTEL}, {@code EXAM}. A {@link FeeStructure} references heads
 * directly (one row per head per applicability).
 */
@Entity
@Table(name = "fee_head",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_head_org_code",
                columnNames = {"organization_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeHead extends OrganizationScopedEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_refundable", nullable = false)
    private boolean refundable;

    @Column(name = "is_taxable", nullable = false)
    private boolean taxable;

    @Column(name = "gl_code", length = 64)
    private String glCode;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GenericStatus status;
}
