package com.thinkerscave.staff.service;

import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Builds staff codes of the form {ORG}-STF-{NNNN} from the staff id, so no separate counter is needed.
 */
@Component
@RequiredArgsConstructor
public class StaffCodeGenerator {

    static final int SHORT_CODE_MAX_LENGTH = 8;
    static final int TRUNCATED_CODE_LENGTH = 4;

    private final OrganizationRepository organizationRepository;

    public String staffCodeFor(Long organizationId, Long staffId) {
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required to generate a staff code");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BadRequestException(
                        "Organization " + organizationId + " is not registered on the platform; "
                                + "staff code prefix cannot be derived"));
        return deriveOrgCode(organization.getShortName()) + "-STF-" + String.format(Locale.ROOT, "%04d", staffId);
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
