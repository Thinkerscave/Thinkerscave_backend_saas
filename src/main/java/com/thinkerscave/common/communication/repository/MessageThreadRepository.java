package com.thinkerscave.common.communication.repository;

import com.thinkerscave.common.communication.domain.MessageThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageThreadRepository extends JpaRepository<MessageThread, Long> {
    Page<MessageThread> findByOrganizationIdOrderByLastMessageAtDesc(Long organizationId, Pageable pageable);
}
