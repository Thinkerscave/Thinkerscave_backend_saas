package com.thinkerscave.common.communication.repository;

import com.thinkerscave.common.communication.domain.NoticeAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeAudienceRepository extends JpaRepository<NoticeAudience, Long> {
    List<NoticeAudience> findByNoticeId(Long noticeId);
    void deleteByNoticeId(Long noticeId);
}
