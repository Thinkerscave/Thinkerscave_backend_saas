package com.thinkerscave.communication.entity;

import com.thinkerscave.communication.enums.NoticeAudienceType;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notice_audience",
        indexes = @Index(name = "idx_notice_audience_notice", columnList = "notice_id"))
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class NoticeAudience extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "audience_id")
    private Long audienceId;

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 30)
    private NoticeAudienceType audienceType;

    @Column(name = "ref_id")
    private Long refId;
}
