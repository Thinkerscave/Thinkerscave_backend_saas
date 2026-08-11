package com.thinkerscave.platform.dto.request;

import com.thinkerscave.platform.enums.BillingCycle;
import com.thinkerscave.platform.enums.InstitutionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for provisioning a new organization under a customer account.
 */
@Getter
@Setter
public class ProvisionOrganizationRequest {

    // ── Customer ──────────────────────────────────────────────────────

    /** Existing customer id. Required for the add-organization flow. */
    private Long existingCustomerId;

    /** New customer fields — used only when existingCustomerId is null */
    @Size(max = 200)
    private String customerLegalName;

    @Size(max = 200)
    private String customerDisplayName;

    @Email
    @Size(max = 150)
    private String customerEmail;

    @Size(max = 20)
    private String customerMobile;

    // ── Organization Details ─────────────────────────────────────────

    @NotBlank
    @Size(max = 200)
    private String organizationName;

    @NotBlank
    @Size(max = 100)
    private String shortName;

    @NotNull
    private InstitutionType institutionType;

    @NotBlank
    @Size(max = 63)
    private String tenantSubdomain;

    @Size(max = 100)
    private String boardName;

    @Size(max = 100)
    private String timeZone;

    @Size(max = 20)
    private String currency;

    @Size(max = 50)
    private String language;

    // ── Step 3: Institution Profile ───────────────────────────────────────────

    @Email
    @Size(max = 150)
    private String orgEmail;

    @Size(max = 20)
    private String orgMobile;

    @Size(max = 255)
    private String addressLine1;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String country;

    @Size(max = 350000)
    private String logoUrl;

    // ── Step 4: Admin User ────────────────────────────────────────────────────

    @NotBlank
    @Size(max = 100)
    private String adminFirstName;

    @Size(max = 100)
    private String adminLastName;

    @NotBlank
    @Email
    @Size(max = 150)
    private String adminEmail;

    @NotBlank
    @Size(max = 20)
    private String adminMobile;

    // ── Step 5: Subscription Plan ─────────────────────────────────────────────

    @NotNull
    private Long subscriptionPlanId;

    @NotNull
    private BillingCycle billingCycle;

    private LocalDate subscriptionStartDate;

    private Boolean trialEnabled;

    // ── Step 6: Feature Customization ────────────────────────────────────────

    private List<Long> enabledFeatureIds;

    private List<Long> disabledFeatureIds;

    // ── Step 7: Commercials ───────────────────────────────────────────────────

    private Long promotionId;

    private String promotionCode;

    private Integer studentLimitOverride;

    private Integer staffLimitOverride;

    private Integer branchLimitOverride;

    private Integer storageLimitOverride;

    // ── Step 8: Provisioning Template ────────────────────────────────────────

    private Long templateId;

    @Size(max = 1000)
    private String remarks;
}
