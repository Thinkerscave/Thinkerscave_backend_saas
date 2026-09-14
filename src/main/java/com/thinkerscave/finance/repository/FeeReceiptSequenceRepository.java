package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeeReceiptSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeeReceiptSequenceRepository extends JpaRepository<FeeReceiptSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM FeeReceiptSequence s WHERE s.sequenceKey = :key")
    Optional<FeeReceiptSequence> findBySequenceKeyForUpdate(@Param("key") String sequenceKey);

    Optional<FeeReceiptSequence> findBySequenceKey(String sequenceKey);
}
