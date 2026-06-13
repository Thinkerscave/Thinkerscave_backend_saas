package com.thinkerscave.shared.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thinkerscave.shared.entity.CodeSequence;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.repository.CodeSequenceRepository;
import com.thinkerscave.shared.service.CodeGeneratorService;

@Service
@RequiredArgsConstructor
public class CodeGeneratorServiceImpl implements CodeGeneratorService {

	private final CodeSequenceRepository repository;

	@Override
	@Transactional
	public String generate(CodeType codeType) {

		CodeSequence sequence = repository.findByCodeType(codeType).orElseGet(() -> createSequence(codeType));

		Long nextValue = sequence.getLastValue() + 1;

		sequence.setLastValue(nextValue);

		repository.save(sequence);

		return buildCode(codeType, nextValue);
	}

	private CodeSequence createSequence(CodeType codeType) {

		CodeSequence sequence = new CodeSequence();

		sequence.setCodeType(codeType);
		sequence.setLastValue(0L);

		return repository.save(sequence);
	}

	private String buildCode(CodeType codeType, Long value) {

		return codeType.getPrefix() + String.format("%06d", value);
	}
}
