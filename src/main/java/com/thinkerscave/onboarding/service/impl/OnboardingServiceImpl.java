package com.thinkerscave.onboarding.service.impl;

import com.thinkerscave.onboarding.dto.OnboardingChecklistResponse;
import com.thinkerscave.onboarding.dto.SetupCategoryResponse;
import com.thinkerscave.onboarding.dto.SetupStatus;
import com.thinkerscave.onboarding.dto.SetupSubtaskResponse;
import com.thinkerscave.onboarding.service.OnboardingService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Organization Setup status derived from real tenant data. Read-only: never creates or edits records.
 * Database errors are propagated (not masked as "not started") so the dashboard can show an error state.
 */
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

        // Organization profile: tenant-local "organizations" row (unqualified, see ProvisionServiceImpl).
        String orgProfileSql = "SELECT COUNT(*) FROM organizations WHERE id = ? "
                + "AND organization_name IS NOT NULL AND TRIM(organization_name) <> '' "
                + "AND email IS NOT NULL AND TRIM(email) <> '' "
                + "AND city IS NOT NULL AND TRIM(city) <> ''";
        boolean profileComplete = count(orgProfileSql, orgId) > 0;
        boolean profileStarted = count("SELECT COUNT(*) FROM organizations WHERE id = ? "
                + "AND (organization_name IS NOT NULL OR email IS NOT NULL OR city IS NOT NULL)", orgId) > 0;

        // Academic setup: tenant-scoped tables (schema-per-tenant, no organization_id column).
        SetupSubtaskResponse academicYear = subtask("academicYear", "Academic Year",
                "Create and set the current academic year.", true,
                statusFor(count("SELECT COUNT(*) FROM academic_year WHERE is_active = true AND status = 'CURRENT'") > 0,
                        count("SELECT COUNT(*) FROM academic_year WHERE is_active = true") > 0),
                "/app/academics/academic-year");
        SetupSubtaskResponse classesSections = subtask("classesSections", "Classes and Sections",
                "Create classes. Sections are optional and can be added later.", true,
                statusFor(count("SELECT COUNT(*) FROM academic_class WHERE is_active = true") > 0, false),
                "/app/academics/classes-sections");
        SetupSubtaskResponse subjects = subtask("subjects", "Subjects",
                "Create the subjects taught in your school.", true,
                statusFor(count("SELECT COUNT(*) FROM subject WHERE is_active = true") > 0, false),
                "/app/academics/subjects-mapping");
        List<SetupSubtaskResponse> academicSubtasks = List.of(academicYear, classesSections, subjects);
        SetupStatus academicStatus = rollUp(academicSubtasks);

        // Staff: at least one active staff member.
        boolean hasStaff = count("SELECT COUNT(*) FROM staff WHERE active = true") > 0;
        // Student: optional. Completed when an active enrollment exists; never blocks academics.
        long activeStudents = count("SELECT COUNT(*) FROM student");
        boolean hasEnrollment = count("SELECT COUNT(*) FROM student_enrollment WHERE active = true AND status = 'ACTIVE'") > 0;

        List<SetupCategoryResponse> categories = List.of(
                SetupCategoryResponse.builder()
                        .key("organizationProfile")
                        .label("Organization Profile")
                        .description("Add your organization's name, contact email, and city.")
                        .mandatory(true)
                        .status(profileComplete ? SetupStatus.COMPLETED
                                : profileStarted ? SetupStatus.IN_PROGRESS : SetupStatus.NOT_STARTED)
                        .route("/app/organization-profile")
                        .subtasks(List.of())
                        .build(),
                SetupCategoryResponse.builder()
                        .key("academicSetup")
                        .label("Academic Setup")
                        .description("Set up the academic year, classes, and subjects.")
                        .mandatory(true)
                        .status(academicStatus)
                        .route("/app/academics/academic-year")
                        .subtasks(academicSubtasks)
                        .build(),
                SetupCategoryResponse.builder()
                        .key("staffSetup")
                        .label("Staff Setup")
                        .description("Add the staff members who will use ThinkersCave.")
                        .mandatory(true)
                        .status(hasStaff ? SetupStatus.COMPLETED : SetupStatus.NOT_STARTED)
                        .route("/app/staff")
                        .subtasks(List.of())
                        .build(),
                SetupCategoryResponse.builder()
                        .key("studentSetup")
                        .label("Student Setup")
                        .description("Add students and enroll them. Can be completed later.")
                        .mandatory(false)
                        .status(hasEnrollment ? SetupStatus.COMPLETED
                                : activeStudents > 0 ? SetupStatus.IN_PROGRESS : SetupStatus.NOT_STARTED)
                        .route("/app/students")
                        .subtasks(List.of())
                        .build()
        );

        int completed = (int) categories.stream().filter(c -> c.getStatus() == SetupStatus.COMPLETED).count();
        int total = categories.size();
        boolean mandatoryComplete = categories.stream()
                .filter(SetupCategoryResponse::isMandatory)
                .allMatch(c -> c.getStatus() == SetupStatus.COMPLETED);

        return OnboardingChecklistResponse.builder()
                .categories(categories)
                .completedCount(completed)
                .totalCount(total)
                .progressPercent((int) Math.round(completed * 100.0 / total))
                .setupComplete(mandatoryComplete)
                .build();
    }

    private SetupSubtaskResponse subtask(String key, String label, String description,
                                         boolean mandatory, SetupStatus status, String route) {
        return SetupSubtaskResponse.builder()
                .key(key)
                .label(label)
                .description(description)
                .mandatory(mandatory)
                .status(status)
                .route(route)
                .build();
    }

    private static SetupStatus statusFor(boolean completed, boolean started) {
        if (completed) {
            return SetupStatus.COMPLETED;
        }
        return started ? SetupStatus.IN_PROGRESS : SetupStatus.NOT_STARTED;
    }

    /** Category is COMPLETED only when every mandatory subtask is COMPLETED. */
    private static SetupStatus rollUp(List<SetupSubtaskResponse> subtasks) {
        boolean allMandatoryDone = subtasks.stream()
                .filter(SetupSubtaskResponse::isMandatory)
                .allMatch(s -> s.getStatus() == SetupStatus.COMPLETED);
        if (allMandatoryDone) {
            return SetupStatus.COMPLETED;
        }
        boolean anyProgress = subtasks.stream().anyMatch(s -> s.getStatus() != SetupStatus.NOT_STARTED);
        return anyProgress ? SetupStatus.IN_PROGRESS : SetupStatus.NOT_STARTED;
    }

    /** Errors are intentionally NOT swallowed; the dashboard maps them to an error state. */
    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value != null ? value : 0L;
    }

}
