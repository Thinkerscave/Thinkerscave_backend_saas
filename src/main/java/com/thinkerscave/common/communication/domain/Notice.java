package com.thinkerscave.common.communication.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Notice / announcement broadcast within the institution. Audience selection
 * details (which classes, roles, users) live in {@link NoticeAudience}.
 */
@Entity
@Table(name = "notice",
        indexes = {
                @Index(name = "idx_notice_org",      columnList = "organization_id"),
                @Index(name = "idx_notice_status",   columnList = "status"),
                @Index(name = "idx_notice_publish",  columnList = "publish_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice extends OrganizationScopedEntity {

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "publish_date", nullable = false)
    private LocalDate publishDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NoticeStatus status;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "published_by_user_id")
    private Long publishedByUserId;
}
