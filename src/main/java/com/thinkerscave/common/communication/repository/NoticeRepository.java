package com.thinkerscave.common.communication.repository;

import com.thinkerscave.common.communication.domain.Notice;
import com.thinkerscave.common.communication.domain.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long>, JpaSpecificationExecutor<Notice> {

    Page<Notice> findByOrganizationIdAndStatus(Long organizationId, NoticeStatus status, Pageable pageable);

    List<Notice> findByOrganizationIdAndStatusAndPublishDateLessThanEqual(
            Long organizationId, NoticeStatus status, LocalDate today);
}
