package com.thinkerscave.communication.service;

import com.thinkerscave.communication.dto.request.NotificationRequest;
import com.thinkerscave.communication.dto.response.NotificationResponse;
import com.thinkerscave.communication.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponse send(NotificationRequest request);

    NotificationResponse getById(Long notificationId);

    Page<NotificationResponse> getAll(Pageable pageable);

    Page<NotificationResponse> getByStatus(NotificationStatus status, Pageable pageable);

    NotificationResponse cancel(Long notificationId);
}
