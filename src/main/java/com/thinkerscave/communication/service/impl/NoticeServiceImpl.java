package com.thinkerscave.communication.service.impl;

import com.thinkerscave.communication.dto.request.NoticeRequest;
import com.thinkerscave.communication.dto.response.NoticeResponse;
import com.thinkerscave.communication.entity.Notice;
import com.thinkerscave.communication.entity.NoticeAudience;
import com.thinkerscave.communication.enums.NoticeStatus;
import com.thinkerscave.communication.repository.NoticeAudienceRepository;
import com.thinkerscave.communication.repository.NoticeRepository;
import com.thinkerscave.communication.service.NoticeService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeAudienceRepository audienceRepository;

    @Override
    @Transactional
    public NoticeResponse create(NoticeRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        Notice notice = new Notice();
        notice.setOrganizationId(orgId);
        mapRequest(request, notice);
        notice = noticeRepository.save(notice);
        saveAudiences(notice, request);
        return toResponse(notice);
    }

    @Override
    @Transactional
    public NoticeResponse update(Long noticeId, NoticeRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        Notice notice = getNotice(noticeId, orgId);
        mapRequest(request, notice);
        audienceRepository.deleteByNoticeId(noticeId);
        notice = noticeRepository.save(notice);
        saveAudiences(notice, request);
        return toResponse(notice);
    }

    @Override
    public NoticeResponse getById(Long noticeId) {
        return toResponse(getNotice(noticeId, OrganizationContext.getOrganizationId()));
    }

    @Override
    public Page<NoticeResponse> getAll(Pageable pageable) {
        return noticeRepository
                .findByOrganizationIdOrderByCreatedOnDesc(OrganizationContext.getOrganizationId(), pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<NoticeResponse> getByStatus(NoticeStatus status, Pageable pageable) {
        return noticeRepository
                .findByOrganizationIdAndStatusOrderByPublishDateDesc(OrganizationContext.getOrganizationId(), status, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<NoticeResponse> getPinnedPublished() {
        return noticeRepository
                .findByOrganizationIdAndStatusAndPinnedTrueOrderByPublishDateDesc(
                        OrganizationContext.getOrganizationId(), NoticeStatus.PUBLISHED)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NoticeResponse publish(Long noticeId) {
        Long orgId = OrganizationContext.getOrganizationId();
        Notice notice = getNotice(noticeId, orgId);
        notice.setStatus(NoticeStatus.PUBLISHED);
        if (notice.getPublishDate() == null) notice.setPublishDate(LocalDate.now());
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return toResponse(noticeRepository.save(notice));
    }

    @Override
    @Transactional
    public void delete(Long noticeId) {
        Long orgId = OrganizationContext.getOrganizationId();
        Notice notice = getNotice(noticeId, orgId);
        audienceRepository.deleteByNoticeId(noticeId);
        noticeRepository.delete(notice);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Notice getNotice(Long noticeId, Long orgId) {
        return noticeRepository.findByNoticeIdAndOrganizationId(noticeId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found: " + noticeId));
    }

    private void mapRequest(NoticeRequest request, Notice notice) {
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setCategory(request.getCategory());
        notice.setPinned(request.isPinned());
        notice.setPublishDate(request.getPublishDate());
        notice.setExpiryDate(request.getExpiryDate());
        notice.setStatus(request.getStatus() != null ? request.getStatus() : NoticeStatus.DRAFT);
        notice.setAttachmentUrl(request.getAttachmentUrl());
    }

    private void saveAudiences(Notice notice, NoticeRequest request) {
        if (request.getAudiences() == null || request.getAudiences().isEmpty()) return;
        request.getAudiences().forEach(entry -> {
            NoticeAudience audience = new NoticeAudience();
            audience.setNoticeId(notice.getNoticeId());
            audience.setAudienceType(entry.getAudienceType());
            audience.setRefId(entry.getRefId());
            audienceRepository.save(audience);
        });
    }

    private NoticeResponse toResponse(Notice n) {
        List<NoticeResponse.AudienceEntryResponse> audiences = audienceRepository
                .findByNoticeId(n.getNoticeId()).stream()
                .map(a -> NoticeResponse.AudienceEntryResponse.builder()
                        .audienceId(a.getAudienceId())
                        .audienceType(a.getAudienceType().name())
                        .refId(a.getRefId())
                        .build())
                .collect(Collectors.toList());

        return NoticeResponse.builder()
                .noticeId(n.getNoticeId())
                .title(n.getTitle())
                .content(n.getContent())
                .category(n.getCategory())
                .pinned(n.isPinned())
                .publishDate(n.getPublishDate())
                .expiryDate(n.getExpiryDate())
                .status(n.getStatus())
                .attachmentUrl(n.getAttachmentUrl())
                .publishedByUserId(n.getPublishedByUserId())
                .createdOn(n.getCreatedOn())
                .createdBy(n.getCreatedBy())
                .audiences(audiences)
                .build();
    }
}
