package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.StaffCodeSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffCodeSequenceRepository extends JpaRepository<StaffCodeSequence, Integer> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO staff_code_sequence (id, last_value, updated_on)
            VALUES (1, 0, NOW())
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StaffCodeSequence s WHERE s.id = 1")
    Optional<StaffCodeSequence> findForUpdate();
}
