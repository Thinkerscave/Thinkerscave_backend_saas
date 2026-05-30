package com.thinkerscave.common.communication.mapper;

import com.thinkerscave.common.communication.domain.Notice;
import com.thinkerscave.common.communication.domain.NoticeAudience;
import com.thinkerscave.common.communication.domain.Notification;
import com.thinkerscave.common.communication.domain.NotificationRecipient;
import com.thinkerscave.common.communication.dto.NoticeAudienceDTO;
import com.thinkerscave.common.communication.dto.NoticeDTO;
import com.thinkerscave.common.communication.dto.NotificationDTO;
import com.thinkerscave.common.communication.dto.NotificationRecipientDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface CommunicationMapper {

    // ── Notice ────────────────────────────────────────────────
    @Mapping(target = "audiences", ignore = true)
    NoticeDTO toNoticeDto(Notice notice);

    List<NoticeDTO> toNoticeDtoList(List<Notice> notices);

    NoticeAudienceDTO toNoticeAudienceDto(NoticeAudience audience);

    List<NoticeAudienceDTO> toNoticeAudienceDtoList(List<NoticeAudience> audiences);

    // ── Notification ──────────────────────────────────────────
    NotificationDTO toNotificationDto(Notification notification);

    List<NotificationDTO> toNotificationDtoList(List<Notification> notifications);

    NotificationRecipientDTO toRecipientDto(NotificationRecipient recipient);

    List<NotificationRecipientDTO> toRecipientDtoList(List<NotificationRecipient> recipients);
}
