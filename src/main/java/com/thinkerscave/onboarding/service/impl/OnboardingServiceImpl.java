package com.thinkerscave.onboarding.service.impl;

import com.thinkerscave.onboarding.dto.OnboardingChecklistItemResponse;
import com.thinkerscave.onboarding.service.OnboardingService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingChecklistItemResponse> getChecklist() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null || orgId <= 0) {
            return List.of();
        }

        long academicYears = countSafe("SELECT COUNT(*) FROM academic_year WHERE organization_id = ?", orgId);
        long classes = countSafe("SELECT COUNT(*) FROM class WHERE organization_id = ?", orgId);
        long sections = countSafe("SELECT COUNT(*) FROM section WHERE organization_id = ?", orgId);
        long subjects = countSafe("SELECT COUNT(*) FROM subject WHERE organization_id = ?", orgId);
        long departments = countSafe("SELECT COUNT(*) FROM departments WHERE organization_id = ?", orgId);
        long staffRoles = countSafe("SELECT COUNT(*) FROM roles WHERE active = true AND role_type = 'STAFF' AND organization_id = ?", orgId);
        long feeStructures = countSafe("SELECT COUNT(*) FROM fee_structure WHERE organization_id = ?", orgId);
        long logoConfigured = countSafe("SELECT COUNT(*) FROM organizations WHERE id = ? AND logo_url IS NOT NULL AND TRIM(logo_url) <> ''", orgId);
        long profileConfigured = countSafe("SELECT COUNT(*) FROM organizations WHERE id = ? AND organization_name IS NOT NULL AND email IS NOT NULL AND city IS NOT NULL", orgId);

        return List.of(
                item("academicYear", "Create Academic Year", academicYears),
                item("classes", "Create Classes", classes),
                item("sections", "Create Sections", sections),
                item("subjects", "Create Subjects", subjects),
                item("departments", "Configure Departments", departments),
                item("staffRoles", "Configure Staff Roles", staffRoles),
                item("feeStructure", "Configure Fee Structure", feeStructures),
                item("logo", "Upload School Logo", logoConfigured),
                item("organizationProfile", "Complete Organization Profile", profileConfigured)
        );
    }

    private OnboardingChecklistItemResponse item(String key, String label, long count) {
        return OnboardingChecklistItemResponse.builder()
                .key(key)
                .label(label)
                .completed(count > 0)
                .count(count)
                .build();
    }

    private long countSafe(String sql, Long orgId) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, orgId);
            return value != null ? value : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
