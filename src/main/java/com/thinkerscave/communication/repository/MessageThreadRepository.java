package com.thinkerscave.communication.repository;

import com.thinkerscave.communication.entity.MessageThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageThreadRepository extends JpaRepository<MessageThread, Long> {

    Page<MessageThread> findByOrganizationIdOrderByLastMessageAtDesc(Long orgId, Pageable pageable);

    Optional<MessageThread> findByThreadIdAndOrganizationId(Long threadId, Long orgId);

    @Query("""
            SELECT t FROM MessageThread t
            WHERE t.organizationId = :orgId
              AND t.closed = false
              AND t.participantUserIdsCsv LIKE %:userId%
            ORDER BY t.lastMessageAt DESC
            """)
    Page<MessageThread> findActiveThreadsForUser(@Param("orgId") Long orgId,
                                                  @Param("userId") String userId,
                                                  Pageable pageable);
}
