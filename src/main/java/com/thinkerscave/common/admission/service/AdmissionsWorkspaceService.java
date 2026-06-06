package com.thinkerscave.common.admission.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thinkerscave.common.admission.domain.ApplicationAdmission;
import com.thinkerscave.common.admission.domain.ApplicationStatus;
import com.thinkerscave.common.admission.domain.CounselingNote;
import com.thinkerscave.common.admission.domain.Inquiry;
import com.thinkerscave.common.admission.dto.AdmissionKpiResponse;
import com.thinkerscave.common.admission.dto.AdmissionProgressResponse;
import com.thinkerscave.common.admission.dto.AdmissionSearchRequest;
import com.thinkerscave.common.admission.dto.AdmissionsSettingsResponse;
import com.thinkerscave.common.admission.dto.AdmissionsSettingsResponse.AdmissionConfig;
import com.thinkerscave.common.admission.dto.AdmissionsSettingsResponse.CounselorAssignmentRules;
import com.thinkerscave.common.admission.dto.AdmissionsSettingsResponse.OptionItem;
import com.thinkerscave.common.admission.dto.ApplicationAdmissionResponse;
import com.thinkerscave.common.admission.dto.CounselingNoteRequest;
import com.thinkerscave.common.admission.dto.CounselingNoteResponse;
import com.thinkerscave.common.admission.dto.FollowUpResponse;
import com.thinkerscave.common.admission.dto.InquiryFullDetailResponse;
import com.thinkerscave.common.admission.dto.InquiryKpiResponse;
import com.thinkerscave.common.admission.dto.InquiryQuickActionsResponse;
import com.thinkerscave.common.admission.dto.InquiryResponse;
import com.thinkerscave.common.admission.dto.InquirySearchRequest;
import com.thinkerscave.common.admission.dto.InquiryTimelineEntry;
import com.thinkerscave.common.admission.enums.FollowUpType;
import com.thinkerscave.common.admission.enums.InquiryStatus;
import com.thinkerscave.common.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.common.admission.repository.CounselingNoteRepository;
import com.thinkerscave.common.admission.repository.FollowUpRepository;
import com.thinkerscave.common.admission.repository.InquiryRepository;
import com.thinkerscave.common.audit.domain.ActivityLog;
import com.thinkerscave.common.audit.service.ActivityLogService;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Backend workspace service for the spec-aligned Admissions module.
 * <p>
 * Provides aggregated payloads consumed directly by the rebuilt
 * Inquiry Center, Inquiry 360 detail workspace, Admission Center
 * and Admissions Settings frontend pages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionsWorkspaceService {

    private static final String INQUIRY_ENTITY = "INQUIRY";
    private static final String COUNSELING_ENTITY = "COUNSELING_NOTE";

    private static final List<String> DEFAULT_REQUIRED_DOCS = List.of(
            "Birth Certificate", "Transfer Certificate", "Aadhaar Card",
            "Passport Photo", "Previous Marksheet", "Address Proof");

    private static final List<String> SPEC_INQUIRY_SOURCES = List.of(
            "WEBSITE", "WALK_IN", "REFERRAL", "PHONE_CALL", "SOCIAL_MEDIA", "CAMPAIGN");

    private final InquiryRepository inquiryRepository;
    private final FollowUpRepository followUpRepository;
    private final CounselingNoteRepository counselingNoteRepository;
    private final ApplicationAdmissionRepository applicationRepository;
    private final ActivityLogService activityLogService;

    @Value("${admissions.admission-number-pattern:ADM-{YYYY}-{SEQ:5}}")
    private String admissionNumberPattern;

    @Value("${admissions.student-id-pattern:STU-{YYYY}-{SEQ:5}}")
    private String studentIdPattern;

    // ============================================================
    // INQUIRY CENTER — KPI & QUICK ACTIONS
    // ============================================================

    @Transactional(readOnly = true)
    public InquiryKpiResponse inquiryKpiSummary() {
        List<Inquiry> all = orgScopedInquiries();
        LocalDate today = LocalDate.now();
        long newCount = countByStatus(all, InquiryStatus.NEW);
        long interested = countByStatus(all, InquiryStatus.INTERESTED) + countByStatus(all, InquiryStatus.CONTACTED);
        long ready = countByStatus(all, InquiryStatus.READY_FOR_ADMISSION);
        long closed = countByStatus(all, InquiryStatus.CLOSED) + countByStatus(all, InquiryStatus.LOST)
                + countByStatus(all, InquiryStatus.CONVERTED);
        long todayFollowUps = all.stream()
                .filter(i -> today.equals(i.getNextFollowUpDate()))
                .count();
        long futureProspects = all.stream()
                .filter(i -> i.getStatus() == InquiryStatus.COUNSELING
                        || i.getStatus() == InquiryStatus.DOCUMENTS_PENDING
                        || i.getStatus() == InquiryStatus.FOLLOW_UP_REQUIRED)
                .count();

        return InquiryKpiResponse.builder()
                .newInquiries(newCount)
                .todaysFollowUps(todayFollowUps)
                .interested(interested)
                .admissionReady(ready)
                .futureProspects(futureProspects)
                .closed(closed)
                .build();
    }

    @Transactional(readOnly = true)
    public InquiryQuickActionsResponse inquiryQuickActions() {
        List<Inquiry> all = orgScopedInquiries();
        LocalDate today = LocalDate.now();
        long todaysCalls = all.stream()
                .filter(i -> today.equals(i.getNextFollowUpDate())
                        && (i.getLastFollowUpType() == null
                                || i.getLastFollowUpType() == FollowUpType.CALL
                                || i.getLastFollowUpType() == FollowUpType.SMS))
                .count();
        long todaysMeetings = all.stream()
                .filter(i -> today.equals(i.getNextFollowUpDate())
                        && i.getLastFollowUpType() == FollowUpType.WALK_IN)
                .count();
        long overdue = all.stream()
                .filter(i -> i.getNextFollowUpDate() != null && i.getNextFollowUpDate().isBefore(today))
                .filter(i -> i.getStatus() != InquiryStatus.LOST
                        && i.getStatus() != InquiryStatus.CONVERTED
                        && i.getStatus() != InquiryStatus.CLOSED)
                .count();
        long ready = countByStatus(all, InquiryStatus.READY_FOR_ADMISSION);

        return InquiryQuickActionsResponse.builder()
                .todaysCalls(todaysCalls)
                .todaysMeetings(todaysMeetings)
                .overdueFollowUps(overdue)
                .admissionReady(ready)
                .build();
    }

    // ============================================================
    // INQUIRY CENTER — SEARCH
    // ============================================================

    @Transactional(readOnly = true)
    public List<InquiryResponse> searchInquiries(InquirySearchRequest filter) {
        List<Inquiry> all = orgScopedInquiries();
        String keyword = filter.getKeyword() == null ? null : filter.getKeyword().trim().toLowerCase();

        return all.stream()
                .filter(i -> filter.getStatus() == null || i.getStatus() == filter.getStatus())
                .filter(i -> filter.getCounselorId() == null
                        || filter.getCounselorId().equals(i.getAssignedCounselorId()))
                .filter(i -> filter.getInquirySource() == null
                        || filter.getInquirySource().equalsIgnoreCase(i.getInquirySource()))
                .filter(i -> filter.getClassInterested() == null
                        || filter.getClassInterested().equalsIgnoreCase(i.getClassInterestedIn()))
                .filter(i -> filter.getFollowUpFrom() == null
                        || (i.getNextFollowUpDate() != null
                                && !i.getNextFollowUpDate().isBefore(filter.getFollowUpFrom())))
                .filter(i -> filter.getFollowUpTo() == null
                        || (i.getNextFollowUpDate() != null
                                && !i.getNextFollowUpDate().isAfter(filter.getFollowUpTo())))
                .filter(i -> {
                    if (keyword == null || keyword.isEmpty()) {
                        return true;
                    }
                    return contains(i.getName(), keyword)
                            || contains(i.getMobileNumber(), keyword)
                            || contains(i.getEmail(), keyword)
                            || contains(String.valueOf(i.getInquiryId()), keyword);
                })
                .sorted(Comparator.comparing(Inquiry::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toInquiryResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // INQUIRY DETAIL — FULL BUNDLE + TIMELINE
    // ============================================================

    @Transactional(readOnly = true)
    public InquiryFullDetailResponse fullDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));

        List<FollowUpResponse> followUps = followUpRepository
                .findByInquiry_InquiryIdOrderByFollowUpDateDesc(inquiryId).stream()
                .map(fu -> FollowUpResponse.builder()
                        .id(fu.getId())
                        .inquiryId(inquiryId)
                        .followUpType(fu.getFollowUpType())
                        .remarks(fu.getRemarks())
                        .statusAfterFollowUp(fu.getStatusAfterFollowUp())
                        .followUpDate(fu.getFollowUpDate())
                        .nextFollowUpDate(fu.getNextFollowUpDate())
                        .createdBy(fu.getCreatedBy())
                        .build())
                .collect(Collectors.toList());

        List<CounselingNoteResponse> counselingNotes = counselingNoteRepository
                .findByInquiry_InquiryIdOrderByCreatedDateDesc(inquiryId).stream()
                .map(this::toCounselingResponse)
                .collect(Collectors.toList());

        List<String> uploaded = List.of();
        List<String> missing = new ArrayList<>(DEFAULT_REQUIRED_DOCS);

        List<InquiryTimelineEntry> timeline = inquiryTimeline(inquiryId);

        return InquiryFullDetailResponse.builder()
                .overview(toInquiryResponse(inquiry))
                .followUps(followUps)
                .counselingNotes(counselingNotes)
                .uploadedDocuments(uploaded)
                .missingDocuments(missing)
                .timeline(timeline)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InquiryTimelineEntry> inquiryTimeline(Long inquiryId) {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            return List.of();
        }
        List<ActivityLog> entries = activityLogService.getActivitiesByType(orgId, INQUIRY_ENTITY).stream()
                .filter(log -> log.getEntityId() != null && log.getEntityId().equals(inquiryId))
                .collect(Collectors.toList());

        return entries.stream()
                .map(log -> InquiryTimelineEntry.builder()
                        .action(log.getAction())
                        .description(log.getDescription())
                        .performedBy(log.getPerformedBy())
                        .performedAt(log.getPerformedAt())
                        .icon(timelineIcon(log.getAction()))
                        .tone(timelineTone(log.getAction()))
                        .build())
                .sorted(Comparator.comparing(InquiryTimelineEntry::getPerformedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    // ============================================================
    // INQUIRY ACTIONS — assign / mark-interested / mark-closed
    // ============================================================

    @Transactional
    public InquiryResponse assignCounselor(Long inquiryId, Long counselorId) {
        Inquiry inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
        inquiry.setAssignedCounselorId(counselorId);
        inquiryRepository.save(inquiry);
        recordInquiryActivity(inquiry, "COUNSELOR_ASSIGNED",
                "Counselor assigned to inquiry " + inquiry.getName());
        return toInquiryResponse(inquiry);
    }

    @Transactional
    public InquiryResponse markInterested(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
        inquiry.setStatus(InquiryStatus.INTERESTED);
        inquiryRepository.save(inquiry);
        recordInquiryActivity(inquiry, "MARKED_INTERESTED",
                "Inquiry " + inquiry.getName() + " marked as interested");
        return toInquiryResponse(inquiry);
    }

    @Transactional
    public InquiryResponse markClosed(Long inquiryId, String reason) {
        Inquiry inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
        inquiry.setStatus(InquiryStatus.CLOSED);
        inquiry.setNextFollowUpDate(null);
        inquiryRepository.save(inquiry);
        recordInquiryActivity(inquiry, "MARKED_CLOSED",
                reason == null || reason.isBlank()
                        ? "Inquiry closed"
                        : "Inquiry closed: " + reason);
        return toInquiryResponse(inquiry);
    }

    // ============================================================
    // COUNSELING NOTES
    // ============================================================

    @Transactional
    public CounselingNoteResponse addCounselingNote(Long inquiryId, CounselingNoteRequest request) {
        Inquiry inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
        CounselingNote note = CounselingNote.builder()
                .inquiry(inquiry)
                .studentRequirements(request.getStudentRequirements())
                .parentConcerns(request.getParentConcerns())
                .campusVisitInfo(request.getCampusVisitInfo())
                .recommendations(request.getRecommendations())
                .notes(request.getNotes())
                .organizationId(OrganizationContext.getOrganizationId())
                .build();
        note = counselingNoteRepository.save(note);
        if (inquiry.getStatus() == InquiryStatus.NEW || inquiry.getStatus() == InquiryStatus.CONTACTED) {
            inquiry.setStatus(InquiryStatus.COUNSELING);
            inquiryRepository.save(inquiry);
        }
        recordInquiryActivity(inquiry, "COUNSELING_ADDED",
                "Counseling note recorded for " + inquiry.getName());
        return toCounselingResponse(note);
    }

    @Transactional(readOnly = true)
    public List<CounselingNoteResponse> counselingNotes(Long inquiryId) {
        return counselingNoteRepository.findByInquiry_InquiryIdOrderByCreatedDateDesc(inquiryId).stream()
                .map(this::toCounselingResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ADMISSION CENTER — KPI / SEARCH / PROGRESS
    // ============================================================

    @Transactional(readOnly = true)
    public AdmissionKpiResponse admissionKpiSummary() {
        List<ApplicationAdmission> all = orgScopedApplications();
        long inProgress = all.stream().filter(a -> a.getStatus() == ApplicationStatus.DRAFT
                || a.getStatus() == ApplicationStatus.PENDING).count();
        long verification = all.stream().filter(a -> a.getStatus() == ApplicationStatus.UNDER_REVIEW).count();
        long docsPending = all.stream().filter(a -> a.getStatus() == ApplicationStatus.PENDING
                && (a.getUploadedDocuments() == null
                        || a.getUploadedDocuments().size() < DEFAULT_REQUIRED_DOCS.size()))
                .count();
        long ready = all.stream().filter(a -> a.getStatus() == ApplicationStatus.UNDER_REVIEW
                && a.getUploadedDocuments() != null
                && a.getUploadedDocuments().size() >= DEFAULT_REQUIRED_DOCS.size()).count();
        long completed = all.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();

        return AdmissionKpiResponse.builder()
                .inProgress(inProgress)
                .documentsPending(docsPending)
                .verificationPending(verification)
                .readyToEnroll(ready)
                .completed(completed)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ApplicationAdmissionResponse> searchAdmissions(AdmissionSearchRequest filter) {
        List<ApplicationAdmission> all = orgScopedApplications();
        String keyword = filter.getKeyword() == null ? null : filter.getKeyword().trim().toLowerCase();
        return all.stream()
                .filter(a -> filter.getAdmissionId() == null
                        || contains(a.getApplicationId(), filter.getAdmissionId().toLowerCase()))
                .filter(a -> filter.getStudentName() == null
                        || contains(a.getApplicantName(), filter.getStudentName().toLowerCase()))
                .filter(a -> filter.getMobileNumber() == null
                        || contains(a.getContactNumber(), filter.getMobileNumber().toLowerCase()))
                .filter(a -> filter.getParentName() == null
                        || contains(a.getParentName(), filter.getParentName().toLowerCase()))
                .filter(a -> filter.getStatus() == null || a.getStatus() == filter.getStatus())
                .filter(a -> filter.getClassApplied() == null
                        || contains(a.getApplyingForSchoolOrCollege(), filter.getClassApplied().toLowerCase()))
                .filter(a -> {
                    if (keyword == null || keyword.isEmpty()) {
                        return true;
                    }
                    return contains(a.getApplicationId(), keyword)
                            || contains(a.getApplicantName(), keyword)
                            || contains(a.getContactNumber(), keyword)
                            || contains(a.getParentName(), keyword)
                            || contains(a.getEmail(), keyword);
                })
                .sorted(Comparator.comparing(ApplicationAdmission::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toAdmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdmissionProgressResponse admissionProgress(String applicationId) {
        ApplicationAdmission app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found: " + applicationId));

        List<Integer> completedSteps = new ArrayList<>();
        List<String> pending = new ArrayList<>();

        boolean step1 = nonBlank(app.getApplicantName()) && app.getDateOfBirth() != null
                && nonBlank(app.getGender()) && nonBlank(app.getApplyingForSchoolOrCollege());
        if (step1) {
            completedSteps.add(1);
        } else {
            pending.add("Basic Information");
        }

        boolean step2 = nonBlank(app.getParentName()) && nonBlank(app.getContactNumber()) && nonBlank(app.getEmail());
        if (step2) {
            completedSteps.add(2);
        } else {
            pending.add("Parent Information");
        }

        boolean step3 = app.getAddress() != null && nonBlank(app.getAddress().getCity())
                && nonBlank(app.getAddress().getPincode());
        if (step3) {
            completedSteps.add(3);
        } else {
            pending.add("Address Information");
        }

        boolean step4 = nonBlank(app.getInternalComments());
        if (step4) {
            completedSteps.add(4);
        } else {
            pending.add("Academic Information");
        }

        boolean step5 = app.getUploadedDocuments() != null && !app.getUploadedDocuments().isEmpty();
        if (step5) {
            completedSteps.add(5);
        } else {
            pending.add("Document Verification");
        }

        boolean step6 = app.getStatus() != null && app.getStatus() != ApplicationStatus.DRAFT;
        if (step6) {
            completedSteps.add(6);
        } else {
            pending.add("Fee Confirmation");
        }

        boolean step7 = app.getStatus() == ApplicationStatus.APPROVED
                || app.getStatus() == ApplicationStatus.UNDER_REVIEW;
        if (step7) {
            completedSteps.add(7);
        } else {
            pending.add("Review & Enrollment");
        }

        int totalSteps = 7;
        int completed = completedSteps.size();
        int current = Math.min(completed + 1, totalSteps);
        int percentage = Math.round((completed * 100f) / totalSteps);

        return AdmissionProgressResponse.builder()
                .applicationId(applicationId)
                .currentStep(current)
                .totalSteps(totalSteps)
                .progressPercentage(percentage)
                .completedSteps(completedSteps)
                .pendingFields(pending)
                .status(app.getStatus() == null ? "DRAFT" : app.getStatus().name())
                .build();
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    @Transactional(readOnly = true)
    public AdmissionsSettingsResponse settings() {
        List<OptionItem> sources = SPEC_INQUIRY_SOURCES.stream()
                .map(code -> OptionItem.builder()
                        .code(code)
                        .label(humanize(code))
                        .active(true)
                        .build())
                .collect(Collectors.toList());

        List<OptionItem> statuses = List.of(InquiryStatus.values()).stream()
                .map(s -> OptionItem.builder()
                        .code(s.name())
                        .label(humanize(s.name()))
                        .active(true)
                        .build())
                .collect(Collectors.toList());

        List<OptionItem> docs = DEFAULT_REQUIRED_DOCS.stream()
                .map(label -> OptionItem.builder()
                        .code(label.toUpperCase().replace(' ', '_'))
                        .label(label)
                        .active(true)
                        .build())
                .collect(Collectors.toList());

        return AdmissionsSettingsResponse.builder()
                .inquirySources(sources)
                .inquiryStatuses(statuses)
                .requiredDocuments(docs)
                .admissionConfig(AdmissionConfig.builder()
                        .autoInquiryNumber(true)
                        .autoAdmissionNumber(true)
                        .admissionNumberPattern(admissionNumberPattern)
                        .studentIdPattern(studentIdPattern)
                        .defaultAdmissionStatus(ApplicationStatus.DRAFT.name())
                        .build())
                .counselorRules(CounselorAssignmentRules.builder()
                        .strategy("MANUAL")
                        .balanceWorkload(false)
                        .considerLocation(false)
                        .build())
                .build();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private List<Inquiry> orgScopedInquiries() {
        Long orgId = OrganizationContext.getOrganizationId();
        List<Inquiry> all = inquiryRepository.findAllByIsDeletedFalseOrderByCreatedDateDesc();
        if (orgId == null) {
            return all;
        }
        return all.stream()
                .filter(i -> i.getOrganizationId() == null || orgId.equals(i.getOrganizationId()))
                .collect(Collectors.toList());
    }

    private List<ApplicationAdmission> orgScopedApplications() {
        Long orgId = OrganizationContext.getOrganizationId();
        List<ApplicationAdmission> all = applicationRepository.findAll();
        if (orgId == null) {
            return all;
        }
        return all.stream()
                .filter(a -> a.getOrganizationId() == null || orgId.equals(a.getOrganizationId()))
                .collect(Collectors.toList());
    }

    private long countByStatus(List<Inquiry> all, InquiryStatus status) {
        return all.stream().filter(i -> i.getStatus() == status).count();
    }

    private boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle);
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String humanize(String code) {
        String lower = code.toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder(lower.length());
        boolean capitalize = true;
        for (char ch : lower.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                sb.append(ch);
                capitalize = true;
            } else if (capitalize) {
                sb.append(Character.toUpperCase(ch));
                capitalize = false;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private String timelineIcon(String action) {
        if (action == null) {
            return "pi pi-circle";
        }
        return switch (action) {
            case "INQUIRY_CREATED" -> "pi pi-sparkles";
            case "FOLLOW_UP_ADDED" -> "pi pi-comments";
            case "COUNSELING_ADDED" -> "pi pi-book";
            case "COUNSELOR_ASSIGNED" -> "pi pi-user-plus";
            case "MARKED_INTERESTED" -> "pi pi-thumbs-up";
            case "MARKED_CLOSED" -> "pi pi-times-circle";
            case "MARKED_LOST" -> "pi pi-flag";
            case "ADMISSION_STARTED", "ADMISSION_CREATED" -> "pi pi-file-edit";
            case "ADMISSION_APPROVED" -> "pi pi-check-circle";
            default -> "pi pi-circle";
        };
    }

    private String timelineTone(String action) {
        if (action == null) {
            return "neutral";
        }
        return switch (action) {
            case "INQUIRY_CREATED", "FOLLOW_UP_ADDED" -> "info";
            case "COUNSELING_ADDED", "COUNSELOR_ASSIGNED" -> "info";
            case "MARKED_INTERESTED", "ADMISSION_APPROVED" -> "success";
            case "MARKED_CLOSED", "MARKED_LOST" -> "danger";
            case "ADMISSION_STARTED", "ADMISSION_CREATED" -> "warning";
            default -> "neutral";
        };
    }

    private void recordInquiryActivity(Inquiry inquiry, String action, String description) {
        try {
            activityLogService.record(
                    OrganizationContext.getOrganizationId() != null
                            ? OrganizationContext.getOrganizationId()
                            : inquiry.getOrganizationId(),
                    INQUIRY_ENTITY,
                    inquiry.getInquiryId(),
                    action,
                    description,
                    SecurityUtil.getCurrentUsername() == null ? "system" : SecurityUtil.getCurrentUsername());
        } catch (Exception ex) {
            log.warn("Failed to record inquiry activity {}: {}", action, ex.getMessage());
        }
    }

    private InquiryResponse toInquiryResponse(Inquiry inquiry) {
        return InquiryResponse.builder()
                .inquiryId(inquiry.getInquiryId())
                .name(inquiry.getName())
                .mobileNumber(inquiry.getMobileNumber())
                .email(inquiry.getEmail())
                .classInterested(inquiry.getClassInterestedIn())
                .address(inquiry.getAddress())
                .inquirySource(inquiry.getInquirySource())
                .referredBy(inquiry.getReferredBy())
                .comments(inquiry.getComments())
                .status(inquiry.getStatus() == null ? null : inquiry.getStatus().name())
                .assignedCounselor(inquiry.getAssignedCounselorId() == null
                        ? null
                        : "Counselor #" + inquiry.getAssignedCounselorId())
                .lastFollowUpDate(inquiry.getLastFollowUpDate())
                .lastFollowUpType(inquiry.getLastFollowUpType() == null ? null : inquiry.getLastFollowUpType().name())
                .nextFollowUpDate(inquiry.getNextFollowUpDate())
                .build();
    }

    private CounselingNoteResponse toCounselingResponse(CounselingNote note) {
        return CounselingNoteResponse.builder()
                .id(note.getId())
                .inquiryId(note.getInquiry() == null ? null : note.getInquiry().getInquiryId())
                .studentRequirements(note.getStudentRequirements())
                .parentConcerns(note.getParentConcerns())
                .campusVisitInfo(note.getCampusVisitInfo())
                .recommendations(note.getRecommendations())
                .notes(note.getNotes())
                .createdBy(note.getCreatedBy())
                .createdAt(note.getCreatedDate())
                .build();
    }

    private ApplicationAdmissionResponse toAdmissionResponse(ApplicationAdmission entity) {
        com.thinkerscave.common.admission.dto.AddressDto addressDto = entity.getAddress() == null
                ? null
                : new com.thinkerscave.common.admission.dto.AddressDto(
                        entity.getAddress().getStreet(),
                        entity.getAddress().getCity(),
                        entity.getAddress().getState(),
                        entity.getAddress().getPincode());
        com.thinkerscave.common.admission.dto.EmergencyContactDto contactDto = entity.getEmergencyContact() == null
                ? null
                : new com.thinkerscave.common.admission.dto.EmergencyContactDto(
                        entity.getEmergencyContact().getName(),
                        entity.getEmergencyContact().getNumber());
        return ApplicationAdmissionResponse.builder()
                .applicationId(entity.getApplicationId())
                .applicantName(entity.getApplicantName())
                .dateOfBirth(entity.getDateOfBirth())
                .gender(entity.getGender())
                .applyingForSchoolOrCollege(entity.getApplyingForSchoolOrCollege())
                .parentName(entity.getParentName())
                .guardianName(entity.getGuardianName())
                .contactNumber(entity.getContactNumber())
                .email(entity.getEmail())
                .address(addressDto)
                .emergencyContact(contactDto)
                .uploadedDocuments(entity.getUploadedDocuments())
                .status(entity.getStatus())
                .internalComments(entity.getInternalComments())
                .build();
    }
}
