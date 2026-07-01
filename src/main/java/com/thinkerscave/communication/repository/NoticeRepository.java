package com.thinkerscave.communication.repository;

import com.thinkerscave.communication.entity.Notice;
import com.thinkerscave.communication.enums.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findByOrganizationIdOrderByCreatedOnDesc(Long orgId, Pageable pageable);

    Page<Notice> findByOrganizationIdAndStatusOrderByPublishDateDesc(Long orgId, NoticeStatus status, Pageable pageable);

    List<Notice> findByOrganizationIdAndStatusAndPinnedTrueOrderByPublishDateDesc(Long orgId, NoticeStatus status);

    List<Notice> findByOrganizationIdAndStatusAndExpiryDateBefore(Long orgId, NoticeStatus status, LocalDate date);

    Optional<Notice> findByNoticeIdAndOrganizationId(Long noticeId, Long orgId);
}
