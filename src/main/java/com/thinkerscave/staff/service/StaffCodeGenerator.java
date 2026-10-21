package com.thinkerscave.staff.service;

import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.staff.entity.StaffCodeSequence;
import com.thinkerscave.staff.repository.StaffCodeSequenceRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Issues staff codes of the form {ORG}-STF-{NNNN}.
 * ORG is derived from the organization short name; the counter is per organization and
 * row-locked, so concurrent creations cannot receive the same number.
 * Must be called inside a transaction (the lock is held until commit).
 */
@Component
@RequiredArgsConstructor
public class StaffCodeGenerator {

    static final int MAX_ATTEMPTS = 1000;
    static final int SHORT_CODE_MAX_LENGTH = 8;
    static final int TRUNCATED_CODE_LENGTH = 4;

    private final OrganizationRepository organizationRepository;
    private final StaffCodeSequenceRepository sequenceRepository;
    private final StaffRepository staffRepository;

    public String nextStaffCode(Long organizationId) {
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required to generate a staff code");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BadRequestException(
                        "Organization " + organizationId + " is not registered on the platform; "
                                + "staff code prefix cannot be derived"));
        String prefix = deriveOrgCode(organization.getShortName());

        sequenceRepository.insertIfAbsent();
        StaffCodeSequence sequence = sequenceRepository.findForUpdate()
                .orElseThrow(() -> new IllegalStateException("Staff code sequence row is missing"));

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            long next = sequence.getLastValue() + 1;
            String code = prefix + "-STF-" + String.format(Locale.ROOT, "%04d", next);
            sequence.setLastValue(next);
            sequence.setUpdatedOn(LocalDateTime.now());
            if (!staffRepository.existsByStaffCode(code)) {
                sequenceRepository.save(sequence);
                return code;
            }
        }
        throw new IllegalStateException("Could not allocate a unique staff code for organization " + organizationId);
    }

    /**
     * Uses the full short name when it is at most 8 alphanumeric characters,
     * otherwise its first 4 alphanumeric characters.
     */
    static String deriveOrgCode(String shortName) {
        if (!StringUtils.hasText(shortName)) {
            throw new BadRequestException("Organization short name is required to generate staff codes");
        }
        String cleaned = shortName.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (cleaned.length() < 2) {
            throw new BadRequestException("Organization short name must contain at least 2 letters or digits");
        }
        return cleaned.length() <= SHORT_CODE_MAX_LENGTH
                ? cleaned
                : cleaned.substring(0, TRUNCATED_CODE_LENGTH);
    }
}
