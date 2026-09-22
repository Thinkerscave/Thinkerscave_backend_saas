package com.thinkerscave.finance.expense.repository;
import com.thinkerscave.finance.expense.entity.ExpenseNumberSequence;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ExpenseNumberSequenceRepository extends JpaRepository<ExpenseNumberSequence,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select s from ExpenseNumberSequence s where s.sequenceKey=:key")
 Optional<ExpenseNumberSequence> findBySequenceKeyForUpdate(@Param("key") String key);
 Optional<ExpenseNumberSequence> findBySequenceKey(String key);
}
