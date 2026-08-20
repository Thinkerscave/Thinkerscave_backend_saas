package com.thinkerscave.onboarding.service.impl;

import com.thinkerscave.onboarding.dto.OnboardingChecklistItemResponse;
import com.thinkerscave.onboarding.dto.OnboardingChecklistResponse;
import com.thinkerscave.onboarding.service.OnboardingService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OnboardingServiceImpl implements OnboardingService {

    private final JdbcTemplate jdbcTemplate;

    public OnboardingServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingChecklistResponse getChecklist() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null || orgId <= 0) {
            throw new BadRequestException("Organization context is required to load onboarding checklist");
        }

        List<OnboardingChecklistItemResponse> items = new ArrayList<>();
        // NOTE: Deliberately unqualified (tenant-local copy, not "public.organizations").
        // Organization Profile self-service edits (OrganizationServiceImpl.updateMyOrganizationProfile)
        // write through the tenant connection, which resolves to this schema-local "organizations" row
        // (materialized at provisioning time — see ProvisionServiceImpl.copyPlatformRows). Reading the
        // qualified public copy here would never reflect those edits, leaving this checklist item stuck
        // on "Pending" forever even after the profile is genuinely completed.
        String orgTable = "organizations";

        // Tenant-scoped academic tables (no organization_id column — schema-per-tenant).
        items.add(item("academicYear", "Create Academic Year",
                countExact("SELECT COUNT(*) FROM academic_year WHERE is_active = true"),
                true, true, "/app/academics/academic-setup"));
        items.add(item("classes", "Create Classes",
                countExact("SELECT COUNT(*) FROM academic_class WHERE is_active = true"),
                true, true, "/app/academics/academic-setup"));
        items.add(item("sections", "Create Sections",
                countExact("SELECT COUNT(*) FROM academic_section WHERE is_active = true"),
                false, true, "/app/academics/academic-setup"));
        items.add(item("subjects", "Create Subjects",
                countExact("SELECT COUNT(*) FROM subject WHERE is_active = true"),
                true, true, "/app/academics/academic-setup"));

        // Not yet buildable in Phase 1 — visible but excluded from completion %.
        items.add(item("departments", "Configure Departments", 0L, false, false, null));
        items.add(item("staffRoles", "Configure Staff Roles",
                countExact("SELECT COUNT(*) FROM roles WHERE active = true AND role_type = 'STAFF'"),
                false, true, "/app/access-management/roles"));
        items.add(item("feeStructure", "Configure Fee Structure", 0L, false, false, null));

        items.add(item("logo", "Upload School Logo",
                countExact("SELECT COUNT(*) FROM " + orgTable + " WHERE id = ? AND logo_url IS NOT NULL AND TRIM(logo_url) <> ''", orgId),
                false, true, "/app/organization-profile"));
        items.add(item("organizationProfile", "Complete Organization Profile",
                countExact("SELECT COUNT(*) FROM " + orgTable + " WHERE id = ? AND organization_name IS NOT NULL AND email IS NOT NULL AND city IS NOT NULL", orgId),
                false, true, "/app/organization-profile"));

        int requiredCount = 0;
        int completedRequired = 0;

        for (OnboardingChecklistItemResponse item : items) {
            if (!item.isAvailable()) {
                continue;
            }
            if (item.isRequiredForCompletion()) {
                requiredCount++;
                if (item.isCompleted()) {
                    completedRequired++;
                }
            }
        }

        OnboardingChecklistItemResponse recommended = items.stream()
                .filter(OnboardingChecklistItemResponse::isAvailable)
                .filter(OnboardingChecklistItemResponse::isRequiredForCompletion)
                .filter(i -> !i.isCompleted())
                .findFirst()
                .orElseGet(() -> items.stream()
                        .filter(OnboardingChecklistItemResponse::isAvailable)
                        .filter(i -> !i.isCompleted())
                        .findFirst()
                        .orElse(null));

        int progressPercent = requiredCount == 0 ? 100 : (int) Math.round((completedRequired * 100.0) / requiredCount);
        boolean setupComplete = requiredCount > 0 && completedRequired >= requiredCount;

        return OnboardingChecklistResponse.builder()
                .items(items)
                .completedRequiredCount(completedRequired)
                .requiredCount(requiredCount)
                .progressPercent(progressPercent)
                .recommendedNextKey(recommended != null ? recommended.getKey() : null)
                .recommendedNextLabel(recommended != null ? recommended.getLabel() : null)
                .recommendedNextRoute(recommended != null ? recommended.getRoute() : null)
                .setupComplete(setupComplete)
                .build();
    }

    private OnboardingChecklistItemResponse item(String key, String label, long count,
                                                 boolean required, boolean available, String route) {
        return OnboardingChecklistItemResponse.builder()
                .key(key)
                .label(label)
                .completed(available && count > 0)
                .count(count)
                .requiredForCompletion(required)
                .available(available)
                .route(route)
                .build();
    }

    private long countExact(String sql, Object... args) {
        try {
            Long value = args.length == 0
                    ? jdbcTemplate.queryForObject(sql, Long.class)
                    : jdbcTemplate.queryForObject(sql, Long.class, args);
            return value != null ? value : 0L;
        } catch (Exception ex) {
            log.warn("Onboarding checklist query skipped: sql={}, error={}", sql, ex.getMessage());
            return 0L;
        }
    }

}
