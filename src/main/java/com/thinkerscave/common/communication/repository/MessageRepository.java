package com.thinkerscave.common.communication.repository;

import com.thinkerscave.common.communication.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByMessageThreadIdOrderBySentAtAsc(Long messageThreadId, Pageable pageable);
    long countByMessageThreadId(Long messageThreadId);
}
