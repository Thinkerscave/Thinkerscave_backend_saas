package com.thinkerscave.shared.service.impl;

import com.thinkerscave.shared.entity.CodeSequence;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.repository.CodeSequenceRepository;
import com.thinkerscave.shared.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CodeGeneratorServiceImpl implements CodeGeneratorService {

	private final CodeSequenceRepository repository;
	private final JdbcTemplate jdbcTemplate;

	@Override
	@Transactional
	public String generate(CodeType codeType) {
		CodeSequence sequence = repository.findByCodeType(codeType).orElseGet(() -> createSequence(codeType));

		long baseline = Math.max(sequence.getLastValue(), resolveExistingMax(codeType));
		long nextValue = baseline + 1;

		sequence.setLastValue(nextValue);
		repository.save(sequence);

		return buildCode(codeType, nextValue);
	}

	private CodeSequence createSequence(CodeType codeType) {
		CodeSequence sequence = new CodeSequence();
		sequence.setCodeType(codeType);
		sequence.setLastValue(resolveExistingMax(codeType));
		return repository.save(sequence);
	}

	/**
	 * Seed/dev data often inserts codes without updating {@code code_sequence}.
	 * Align the counter with the highest existing code so new inserts never collide.
	 */
	private long resolveExistingMax(CodeType codeType) {
		String sql = switch (codeType) {
			case CUSTOMER -> "SELECT COALESCE(MAX(CAST(SUBSTRING(customer_code, 4) AS BIGINT)), 0) FROM customers";
			case ORGANIZATION -> "SELECT COALESCE(MAX(CAST(SUBSTRING(organization_code, 4) AS BIGINT)), 0) FROM organizations";
			case USER -> "SELECT COALESCE(MAX(CAST(SUBSTRING(user_code, 4) AS BIGINT)), 0) FROM users";
			case CONTACT -> "SELECT COALESCE(MAX(CAST(SUBSTRING(contact_code, 4) AS BIGINT)), 0) FROM customer_contacts";
			case TENANT -> "SELECT COALESCE(MAX(CAST(SUBSTRING(tenant_identifier, 4) AS BIGINT)), 0) FROM tenant_registry";
			case PROVISION_JOB -> "SELECT COALESCE(MAX(CAST(SUBSTRING(job_code, 4) AS BIGINT)), 0) FROM provisioning_jobs";
			case PROMOTION -> "SELECT COALESCE(MAX(CAST(SUBSTRING(promotion_code, 4) AS BIGINT)), 0) FROM promotions";
			case TEMPLATE -> "SELECT COALESCE(MAX(CAST(SUBSTRING(template_code, 4) AS BIGINT)), 0) FROM provisioning_templates";
			default -> null;
		};
		if (sql == null) {
			return 0L;
		}
		try {
			Long max = jdbcTemplate.queryForObject(sql, Long.class);
			return max != null ? max : 0L;
		} catch (Exception ignored) {
			// Table may not exist yet for some code types in fresh environments.
			return 0L;
		}
	}

	private String buildCode(CodeType codeType, Long value) {
		return codeType.getPrefix() + String.format("%06d", value);
	}
}
