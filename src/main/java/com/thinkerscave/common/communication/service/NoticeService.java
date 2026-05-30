package com.thinkerscave.common.communication.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.communication.domain.Notice;
import com.thinkerscave.common.communication.domain.NoticeAudience;
import com.thinkerscave.common.communication.domain.NoticeStatus;
import com.thinkerscave.common.communication.dto.NoticeAudienceDTO;
import com.thinkerscave.common.communication.dto.NoticeDTO;
import com.thinkerscave.common.communication.mapper.CommunicationMapper;
import com.thinkerscave.common.communication.repository.NoticeAudienceRepository;
import com.thinkerscave.common.communication.repository.NoticeRepository;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Notice/announcement workflow — create as DRAFT, schedule or publish,
 * targeting an audience set.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeAudienceRepository audienceRepository;
    private final AuditPublisher auditPublisher;
    private final CommunicationMapper communicationMapper;

    public Page<NoticeDTO> listByStatus(NoticeStatus status, Pageable pageable) {
        return noticeRepository.findByOrganizationIdAndStatus(currentOrgId(), status, pageable)
                .map(communicationMapper::toNoticeDto);
    }

    public List<NoticeDTO> activeForToday() {
        return noticeRepository.findByOrganizationIdAndStatusAndPublishDateLessThanEqual(
                currentOrgId(), NoticeStatus.PUBLISHED, LocalDate.now())
                .stream().map(communicationMapper::toNoticeDto).toList();
    }

    public NoticeDTO get(Long id) {
        Notice n = load(id);
        NoticeDTO dto = communicationMapper.toNoticeDto(n);
        dto.setAudiences(audienceRepository.findByNoticeId(id).stream().map(communicationMapper::toNoticeAudienceDto).toList());
        return dto;
    }

    @Transactional
    public NoticeDTO save(NoticeDTO dto) {
        Long orgId = currentOrgId();
        Notice n;
        boolean creating = dto.getId() == null;
        if (creating) {
            n = new Notice();
            n.setOrganizationId(orgId);
        } else {
            n = load(dto.getId());
            if (n.getStatus() == NoticeStatus.PUBLISHED || n.getStatus() == NoticeStatus.ARCHIVED) {
                throw new BadRequestException("Cannot edit a " + n.getStatus() + " notice");
            }
        }
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        n.setCategory(dto.getCategory());
        n.setPinned(dto.isPinned());
        n.setPublishDate(dto.getPublishDate());
        n.setExpiryDate(dto.getExpiryDate());
        n.setStatus(dto.getStatus() != null ? dto.getStatus() : NoticeStatus.DRAFT);
        n.setAttachmentUrl(dto.getAttachmentUrl());
        Notice saved = noticeRepository.save(n);

        if (!creating) audienceRepository.deleteByNoticeId(saved.getId());
        if (dto.getAudiences() != null) {
            for (NoticeAudienceDTO a : dto.getAudiences()) {
                NoticeAudience na = new NoticeAudience();
                na.setNoticeId(saved.getId());
                na.setAudienceType(a.getAudienceType());
                na.setRefId(a.getRefId());
                audienceRepository.save(na);
            }
        }

        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "notice.create" : "notice.update",
                "Notice", saved.getId(), "Notice " + saved.getTitle());
        return get(saved.getId());
    }

    @Transactional
    public NoticeDTO publish(Long id, Long byUserId) {
        Notice n = load(id);
        if (n.getStatus() != NoticeStatus.DRAFT && n.getStatus() != NoticeStatus.SCHEDULED) {
            throw new BadRequestException("Cannot publish a notice in status " + n.getStatus());
        }
        n.setStatus(NoticeStatus.PUBLISHED);
        n.setPublishedByUserId(byUserId);
        if (n.getPublishDate() == null) n.setPublishDate(LocalDate.now());
        Notice saved = noticeRepository.save(n);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "notice.publish",
                "Notice", id, "Notice published: " + n.getTitle());
        return communicationMapper.toNoticeDto(saved);
    }

    @Transactional
    public NoticeDTO archive(Long id) {
        Notice n = load(id);
        n.setStatus(NoticeStatus.ARCHIVED);
        Notice saved = noticeRepository.save(n);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "notice.archive",
                "Notice", id, "Notice archived");
        return communicationMapper.toNoticeDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Notice n = load(id);
        if (n.getStatus() == NoticeStatus.PUBLISHED) {
            throw new BadRequestException("Cannot delete a PUBLISHED notice — archive it instead");
        }
        audienceRepository.deleteByNoticeId(id);
        noticeRepository.delete(n);
        auditPublisher.publish(AuditEventType.DELETE, "notice.delete",
                "Notice", id, "Notice deleted: " + n.getTitle());
    }

    private Notice load(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found: " + id));
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
