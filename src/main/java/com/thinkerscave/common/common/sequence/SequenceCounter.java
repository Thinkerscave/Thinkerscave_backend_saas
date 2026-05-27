package com.thinkerscave.common.common.sequence;

import com.thinkerscave.common.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Centralized sequence counter — supports per-organization, per-context
 * monotonic number generation (admission numbers, invoice numbers,
 * receipt numbers, enrollment numbers, etc.).
 *
 * <p>Identified by ({@code organizationId}, {@code sequenceKey}). The
 * {@code lastNumber} column is updated atomically by
 * {@link SequenceGeneratorService}.
 */
@Entity
@Table(name = "sequence_counter",
        uniqueConstraints = @UniqueConstraint(name = "uk_sequence_counter_org_key",
                columnNames = {"organization_id", "sequence_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceCounter extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** e.g. {@code ADMISSION}, {@code INVOICE}, {@code RECEIPT}, {@code ENROLLMENT}. */
    @Column(name = "sequence_key", nullable = false, length = 64)
    private String sequenceKey;

    /** Optional context — e.g. academic year code, {@code "2026-27"}. */
    @Column(name = "context", length = 64)
    private String context;

    @Column(name = "prefix", length = 16)
    private String prefix;

    @Column(name = "last_number", nullable = false)
    private Long lastNumber = 0L;

    @Column(name = "padding")
    private Integer padding = 5;
}
