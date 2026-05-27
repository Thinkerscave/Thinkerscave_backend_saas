package com.thinkerscave.common.common.sequence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface SequenceCounterRepository extends JpaRepository<SequenceCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SequenceCounter> findByOrganizationIdAndSequenceKeyAndContext(
            Long organizationId, String sequenceKey, String context);
}
