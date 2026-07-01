package com.thinkerscave.communication.service;

import com.thinkerscave.communication.dto.request.NoticeRequest;
import com.thinkerscave.communication.dto.response.NoticeResponse;
import com.thinkerscave.communication.enums.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NoticeService {

    NoticeResponse create(NoticeRequest request);

    NoticeResponse update(Long noticeId, NoticeRequest request);

    NoticeResponse getById(Long noticeId);

    Page<NoticeResponse> getAll(Pageable pageable);

    Page<NoticeResponse> getByStatus(NoticeStatus status, Pageable pageable);

    List<NoticeResponse> getPinnedPublished();

    NoticeResponse publish(Long noticeId);

    void delete(Long noticeId);
}
