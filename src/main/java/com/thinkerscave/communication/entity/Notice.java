package com.thinkerscave.communication.entity;

import com.thinkerscave.communication.enums.NoticeStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "notice",
        indexes = {
            @Index(name = "idx_notice_org_status", columnList = "organization_id, status"),
            @Index(name = "idx_notice_publish_date", columnList = "publish_date")
        })
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Notice extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "pinned")
    private boolean pinned = false;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NoticeStatus status = NoticeStatus.DRAFT;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "published_by_user_id")
    private Long publishedByUserId;
}
