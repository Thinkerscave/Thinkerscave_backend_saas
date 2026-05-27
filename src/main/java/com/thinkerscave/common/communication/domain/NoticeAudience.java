package com.thinkerscave.common.communication.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Audience targeting row for a {@link Notice}. {@code refId} is interpreted
 * based on {@link #audienceType} (role id, class id, section id, user id, …).
 * A notice may have multiple audience rows.
 */
@Entity
@Table(name = "notice_audience",
        indexes = {
                @Index(name = "idx_notice_aud_notice", columnList = "notice_id"),
                @Index(name = "idx_notice_aud_ref",    columnList = "audience_type,ref_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeAudience extends AuditableBaseEntity {

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 24)
    private NoticeAudienceType audienceType;

    @Column(name = "ref_id")
    private Long refId;
}
