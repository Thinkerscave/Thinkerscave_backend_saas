package com.thinkerscave.shared.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.thinkerscave.shared.entity.CodeSequence;
import com.thinkerscave.shared.enums.CodeType;

import jakarta.persistence.LockModeType;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, CodeType> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<CodeSequence> findByCodeType(CodeType codeType);
}
