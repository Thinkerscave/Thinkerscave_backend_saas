package com.thinkerscave.security.service;

import com.thinkerscave.access.dto.UserProvisioningResult;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.repository.OrganizationDomainRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * Sends first-time login credentials to institution users (staff, students) after their creation commits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCredentialNotificationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationDomainRepository organizationDomainRepository;
    private final OutboundMessageService outboundMessageService;

    @Value("${app.platform.login-url:http://localhost:4200/auth/login}")
    private String fallbackLoginUrl;

    public void sendInstitutionUserCredentials(
            UserProvisioningResult provisioning,
            String recipientEmail,
            String recipientMobile,
            String roleLabel) {
        if (!StringUtils.hasText(recipientEmail) && !StringUtils.hasText(recipientMobile)) {
            log.info("Credentials not sent: no email or mobile for role={}", roleLabel);
            return;
        }

        User user = provisioning.getUser();
        Long organizationId = OrganizationContext.getOrganizationId();
        String organizationName = organizationId == null ? null
                : organizationRepository.findById(organizationId).map(Organization::getOrganizationName).orElse(null);
        String loginUrl = resolveLoginUrl(organizationId);
        String recipientName = StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : user.getDisplayName();

        Runnable dispatch = () -> {
            try {
                outboundMessageService.sendInstitutionUserCredentials(
                        recipientEmail, recipientMobile, recipientName, roleLabel,
                        organizationName, loginUrl, user.getUsername(), provisioning.getTemporaryPassword());
            } catch (Exception ex) {
                log.error("Credential notification failed for role={} org={}: {}",
                        roleLabel, organizationId, ex.getMessage(), ex);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    private String resolveLoginUrl(Long organizationId) {
        if (organizationId == null) {
            return fallbackLoginUrl;
        }
        String baseUrl = organizationDomainRepository.findByOrganization_Id(organizationId)
                .map(domain -> "https://" + domain.getDomain() + "/auth/login")
                .orElse(fallbackLoginUrl);
        return baseUrl + "?org=" + organizationId;
    }
}
