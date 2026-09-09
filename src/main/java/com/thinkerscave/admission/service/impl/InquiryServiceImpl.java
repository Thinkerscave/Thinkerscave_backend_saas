package com.thinkerscave.admission.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.admission.dto.request.CompleteFollowUpRequest;
import com.thinkerscave.admission.dto.request.CounselingNoteRequest;
import com.thinkerscave.admission.dto.request.FollowUpRequest;
import com.thinkerscave.admission.dto.request.InquiryRequest;
import com.thinkerscave.admission.dto.request.LeadSearchRequest;
import com.thinkerscave.admission.dto.response.AdmissionKpiResponse;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.dto.response.CounselingNoteResponse;
import com.thinkerscave.admission.dto.response.FollowUpResponse;
import com.thinkerscave.admission.dto.response.InquiryFullDetailResponse;
import com.thinkerscave.admission.dto.response.InquiryQuickActionResponse;
import com.thinkerscave.admission.dto.response.InquiryResponse;
import com.thinkerscave.admission.dto.response.InquiryTimelineItemResponse;
import com.thinkerscave.admission.dto.response.InquiryWorkspaceKpiResponse;
import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.admission.entity.CounselingNote;
import com.thinkerscave.admission.entity.Inquiry;
import com.thinkerscave.admission.entity.InquiryFollowUp;
import com.thinkerscave.admission.entity.LeadCounselorAssignment;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.FollowUpLifecycleStatus;
import com.thinkerscave.admission.enums.FollowUpType;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.enums.LeadSource;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.repository.CounselingNoteRepository;
import com.thinkerscave.admission.repository.InquiryFollowUpRepository;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.admission.repository.LeadCounselorAssignmentRepository;
import com.thinkerscave.admission.service.AdmissionsSettingService;
import com.thinkerscave.admission.service.InquiryService;
import com.thinkerscave.admission.specification.InquirySpecification;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.audit.entity.AuditLog;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.repository.AuditLogRepository;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.response.StaffSummaryResponse;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.entity.ResponsibilityAssignment;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.repository.ResponsibilityAssignmentRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InquiryServiceImpl implements InquiryService {

    private static final String COUNSELOR_RESPONSIBILITY_CODE = "COUNSELOR";
    private static final String RESOURCE_ADMISSIONS_LEADS = "ADMISSIONS_LEADS";

    private final InquiryRepository inquiryRepository;
    private final InquiryFollowUpRepository followUpRepository;
    private final CounselingNoteRepository counselingNoteRepository;
    private final ApplicationAdmissionRepository applicationRepository;
    private final LeadCounselorAssignmentRepository leadCounselorAssignmentRepository;
    private final ClassRepository classRepository;
    private final AdmissionsSettingService settingService;
    private final ResponsibilityAssignmentRepository responsibilityAssignmentRepository;
    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final AuditWriteService auditWriteService;
    private final AuditLogRepository auditLogRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public InquiryResponse create(InquiryRequest request) {
        validateLeadRequest(request);
        String mobile = request.getMobileNumber().trim();
        if (inquiryRepository.existsByMobileNumberAndDeletedFalse(mobile)
            && !Boolean.TRUE.equals(request.getAllowPotentialDuplicate())) {
            throw new BadRequestException("A lead with this mobile number already exists");
        }

        AcademicClass academicClass = requireActiveClass(request.getClassId(), request.getAcademicYearId());

        Inquiry inquiry = new Inquiry();
        mapRequest(request, inquiry);
        inquiry.setClassInterestedIn(academicClass.getName());
        inquiry.setStatus(InquiryStatus.NEW);
        inquiry.setDeleted(false);
        inquiry.setInquiryNumber(generateInquiryNumber());

        Inquiry saved = inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.CREATE, "LEAD_CREATED", "INQUIRY", String.valueOf(saved.getInquiryId()),
            "Lead created: " + saved.getStudentName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InquiryResponse update(Long inquiryId, InquiryRequest request) {
        Inquiry inquiry = getInquiry(inquiryId);
        validateLeadRequest(request);
        AcademicClass academicClass = requireActiveClass(request.getClassId(), request.getAcademicYearId());
        mapRequest(request, inquiry);
        inquiry.setClassInterestedIn(academicClass.getName());
        Inquiry saved = inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.UPDATE, "LEAD_UPDATED", "INQUIRY", String.valueOf(saved.getInquiryId()),
                "Lead updated: " + saved.getStudentName());
        return toResponse(saved);
    }

    @Override
    public InquiryResponse getById(Long inquiryId) {
        return toResponse(getInquiry(inquiryId));
    }

    @Override
    public Page<InquiryResponse> getAll(Pageable pageable) {
        LeadSearchRequest enforced = applyVisibilityScope(new LeadSearchRequest());
        return inquiryRepository
            .findAll(InquirySpecification.filter(enforced), pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<InquiryResponse> search(LeadSearchRequest request, Pageable pageable) {
        LeadSearchRequest enforced = applyVisibilityScope(request == null ? new LeadSearchRequest() : request);
        return inquiryRepository.findAll(InquirySpecification.filter(enforced), pageable)
                .map(this::toResponse);
    }

    @Override
    public List<InquiryResponse> getByStatus(InquiryStatus status) {
        return inquiryRepository
                .findByStatusAndDeletedFalseOrderByCreatedOnDesc(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<InquiryResponse> getPendingFollowUps() {
        return inquiryRepository
                .findByDeletedFalseAndNextFollowUpDateLessThanEqualOrderByNextFollowUpDateAsc(LocalDate.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void softDelete(Long inquiryId) {
        requireManageLeads();
        Inquiry inquiry = getInquiry(inquiryId);
        inquiry.setDeleted(true);
        inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "LEAD_ARCHIVED", "INQUIRY", String.valueOf(inquiryId),
                "Lead archived: " + inquiry.getStudentName());
    }

    @Override
    @Transactional
    public InquiryResponse updateStatus(Long inquiryId, InquiryStatus newStatus) {
        requireManageLeads();
        Inquiry inquiry = getInquiry(inquiryId);
        inquiry.setStatus(newStatus);
        Inquiry saved = inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "LEAD_STATUS_CHANGED", "INQUIRY", String.valueOf(inquiryId),
                "Lead status changed to " + newStatus.name());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InquiryResponse markLost(Long inquiryId, String reason) {
        Inquiry inquiry = getInquiry(inquiryId);
        inquiry.setStatus(InquiryStatus.LOST);
        if (reason != null && !reason.isBlank()) {
            String existing = inquiry.getComments() == null ? "" : inquiry.getComments().trim();
            inquiry.setComments((existing.isEmpty() ? "" : existing + " | ") + "Lost Reason: " + reason.trim());
        }
        Inquiry saved = inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "LEAD_MARKED_LOST", "INQUIRY", String.valueOf(inquiryId),
            "Lead marked lost");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InquiryResponse assignCounselor(Long inquiryId, Long counselorId, String reason) {
        requireManageLeads();
        Inquiry inquiry = getInquiry(inquiryId);
        Staff counselor = requireEligibleCounselor(counselorId);
        Long previous = inquiry.getAssignedCounselorId();
        inquiry.setAssignedCounselorId(counselorId);
        Inquiry saved = inquiryRepository.save(inquiry);

        leadCounselorAssignmentRepository.findByInquiryIdAndActiveTrueOrderByAssignedOnDesc(inquiryId)
                .forEach(item -> {
                    item.setActive(false);
                    leadCounselorAssignmentRepository.save(item);
                });

        LeadCounselorAssignment history = new LeadCounselorAssignment();
        history.setInquiryId(inquiryId);
        history.setPreviousCounselorStaffId(previous);
        history.setNewCounselorStaffId(counselor.getStaffId());
        history.setReason(StringUtils.hasText(reason) ? reason.trim() : null);
        history.setAssignedByUserId(currentUserId());
        history.setAssignedByUsername(currentUsername());
        history.setAssignedOn(LocalDateTime.now());
        history.setActive(true);
        leadCounselorAssignmentRepository.save(history);

        String event = previous == null ? "COUNSELOR_ASSIGNED" : "COUNSELOR_REASSIGNED";
        String summary = previous == null
                ? "Counselor assigned: " + staffDisplayName(counselor)
                : "Counselor reassigned to " + staffDisplayName(counselor);
        auditWriteService.record(AuditEventType.STATE_CHANGE, event, "INQUIRY", String.valueOf(inquiryId), summary);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationAdmissionResponse convertToApplication(Long inquiryId) {
        Inquiry inquiry = getInquiry(inquiryId);

        if (applicationRepository.existsByInquiryId(inquiryId)) {
            ApplicationAdmission existing = applicationRepository.findByInquiryId(inquiryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found for inquiry: " + inquiryId));
            return toApplicationResponse(existing);
        }

        if (inquiry.getStatus() == InquiryStatus.LOST) {
            throw new BadRequestException("Lost leads cannot be converted to applications");
        }

        ApplicationAdmission app = new ApplicationAdmission();
        app.setInquiryId(inquiry.getInquiryId());
        app.setApplicationNumber(generateApplicationNumber());
        app.setApplicantName(inquiry.getStudentName());
        app.setApplyingForClass(inquiry.getClassInterestedIn());
        app.setAcademicYearId(inquiry.getAcademicYearId());
        app.setClassId(inquiry.getClassId());
        app.setEmail(inquiry.getEmail());
        app.setContactNumber(inquiry.getMobileNumber());
        app.setAddress(inquiry.getAddress());
        app.setParentName(inquiry.getParentContactName());
        app.setParentContact(inquiry.getMobileNumber());
        app.setInternalComments(inquiry.getComments());
        app.setStatus(ApplicationStatus.DRAFT);
        ApplicationAdmission saved = applicationRepository.save(app);

        inquiry.setStatus(InquiryStatus.APPLICATION_STARTED);
        inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "APPLICATION_STARTED", "INQUIRY", String.valueOf(inquiryId),
            "Application started from lead");
        return toApplicationResponse(saved);
    }

    @Override
    @Transactional
    public FollowUpResponse addFollowUp(Long inquiryId, FollowUpRequest request) {
        Inquiry inquiry = getInquiry(inquiryId);

        InquiryFollowUp followUp = new InquiryFollowUp();
        followUp.setInquiry(inquiry);
        followUp.setFollowUpType(request.getFollowUpType());
        followUp.setRemarks(request.getRemarks());
        followUp.setStatusAfter(request.getStatusAfter());
        followUp.setFollowUpDate(request.getFollowUpDate() != null ? request.getFollowUpDate() : LocalDateTime.now());
        followUp.setNextFollowUpDate(request.getNextFollowUpDate());
        followUp.setLifecycleStatus(FollowUpLifecycleStatus.SCHEDULED);
        followUp = followUpRepository.save(followUp);

        // Update inquiry state
        inquiry.setLastFollowUpDate(followUp.getFollowUpDate());
        inquiry.setLastFollowUpType(followUp.getFollowUpType());
        if (request.getNextFollowUpDate() != null) inquiry.setNextFollowUpDate(request.getNextFollowUpDate());
        if (request.getStatusAfter() != null) inquiry.setStatus(request.getStatusAfter());
        inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.UPDATE, "FOLLOW_UP_SCHEDULED", "INQUIRY", String.valueOf(inquiryId),
            "Follow-up scheduled: " + followUp.getFollowUpType());

        return toFollowUpResponse(followUp);
    }

    @Override
    public List<FollowUpResponse> getFollowUps(Long inquiryId) {
        return followUpRepository
                .findByInquiryInquiryIdOrderByFollowUpDateDesc(inquiryId)
                .stream().map(this::toFollowUpResponse).collect(Collectors.toList());
    }

    @Override
    public List<FollowUpResponse> getTodayFollowUps() {
        return followUpRepository.findDueOnDate(LocalDate.now())
                .stream().map(this::toFollowUpResponse).collect(Collectors.toList());
    }

    @Override
    public List<FollowUpResponse> getOverdueFollowUps() {
        return followUpRepository.findOverdue(LocalDate.now())
                .stream().map(this::toFollowUpResponse).collect(Collectors.toList());
    }

    @Override
    public List<FollowUpResponse> getUpcomingFollowUps() {
        return followUpRepository.findUpcoming(LocalDate.now())
                .stream().map(this::toFollowUpResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FollowUpResponse updateFollowUp(Long followUpId, FollowUpRequest request) {
        InquiryFollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found: " + followUpId));

        if (request.getFollowUpType() != null) {
            followUp.setFollowUpType(request.getFollowUpType());
        }
        followUp.setRemarks(request.getRemarks());
        followUp.setStatusAfter(request.getStatusAfter());
        if (request.getFollowUpDate() != null) {
            followUp.setFollowUpDate(request.getFollowUpDate());
        }
        followUp.setNextFollowUpDate(request.getNextFollowUpDate());
        if (request.getNextFollowUpDate() != null
                && followUp.getLifecycleStatus() != FollowUpLifecycleStatus.COMPLETED
                && followUp.getLifecycleStatus() != FollowUpLifecycleStatus.CANCELLED) {
            followUp.setLifecycleStatus(FollowUpLifecycleStatus.RESCHEDULED);
        }
        followUp = followUpRepository.save(followUp);

        Inquiry inquiry = followUp.getInquiry();
        if (followUp.getStatusAfter() != null) {
            inquiry.setStatus(followUp.getStatusAfter());
        }
        inquiry.setLastFollowUpDate(followUp.getFollowUpDate());
        inquiry.setLastFollowUpType(followUp.getFollowUpType());
        inquiry.setNextFollowUpDate(followUp.getNextFollowUpDate());
        inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.UPDATE, "FOLLOW_UP_UPDATED", "INQUIRY", String.valueOf(inquiry.getInquiryId()),
            "Follow-up updated");
        return toFollowUpResponse(followUp);
    }

    @Override
    @Transactional
    public FollowUpResponse completeFollowUp(Long followUpId) {
        return completeFollowUp(followUpId, new CompleteFollowUpRequest());
    }

    @Override
    @Transactional
    public FollowUpResponse completeFollowUp(Long followUpId, CompleteFollowUpRequest request) {
        InquiryFollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found: " + followUpId));
        Inquiry inquiry = followUp.getInquiry();
        followUp.setLifecycleStatus(FollowUpLifecycleStatus.COMPLETED);
        followUp.setCompletedOn(LocalDateTime.now());
        followUp.setCompletedBy(currentUsername());
        if (request != null) {
            if (request.getOutcome() != null) {
                followUp.setOutcome(request.getOutcome());
            }
            if (request.getRemarks() != null) {
                followUp.setRemarks(request.getRemarks());
            }
            if (request.getStatusAfter() != null) {
                followUp.setStatusAfter(request.getStatusAfter());
                inquiry.setStatus(request.getStatusAfter());
            }
            if (request.getNextFollowUpDate() != null) {
                followUp.setNextFollowUpDate(request.getNextFollowUpDate());
                inquiry.setNextFollowUpDate(request.getNextFollowUpDate());
            } else {
                inquiry.setNextFollowUpDate(null);
            }
        } else {
            inquiry.setNextFollowUpDate(null);
        }
        inquiry.setLastFollowUpDate(LocalDateTime.now());
        inquiry.setLastFollowUpType(followUp.getFollowUpType());
        inquiryRepository.save(inquiry);
        FollowUpResponse response = toFollowUpResponse(followUpRepository.save(followUp));
        auditWriteService.record(AuditEventType.UPDATE, "FOLLOW_UP_COMPLETED", "INQUIRY", String.valueOf(inquiry.getInquiryId()),
            "Follow-up completed");
        return response;
    }

    @Override
    @Transactional
    public FollowUpResponse cancelFollowUp(Long followUpId, String remarks) {
        InquiryFollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found: " + followUpId));
        followUp.setLifecycleStatus(FollowUpLifecycleStatus.CANCELLED);
        followUp.setCompletedOn(LocalDateTime.now());
        followUp.setCompletedBy(currentUsername());
        if (remarks != null && !remarks.isBlank()) {
            followUp.setRemarks(remarks.trim());
        }
        Inquiry inquiry = followUp.getInquiry();
        inquiry.setNextFollowUpDate(null);
        inquiryRepository.save(inquiry);
        FollowUpResponse response = toFollowUpResponse(followUpRepository.save(followUp));
        auditWriteService.record(AuditEventType.UPDATE, "FOLLOW_UP_CANCELLED", "INQUIRY", String.valueOf(inquiry.getInquiryId()),
            "Follow-up cancelled");
        return response;
    }

    @Override
    @Transactional
    public CounselingNoteResponse addCounselingNote(Long inquiryId, CounselingNoteRequest request) {
        Inquiry inquiry = getInquiry(inquiryId);

        CounselingNote note = new CounselingNote();
        note.setInquiry(inquiry);
        note.setStudentRequirements(request.getStudentRequirements());
        note.setParentConcerns(request.getParentConcerns());
        note.setCampusVisitInfo(request.getCampusVisitInfo());
        note.setRecommendations(request.getRecommendations());
        note.setNotes(request.getNotes());
        CounselingNote saved = counselingNoteRepository.save(note);
        auditWriteService.record(AuditEventType.UPDATE, "COUNSELING_ADDED", "INQUIRY", String.valueOf(inquiryId),
            "Counseling note added");
        return toCounselingResponse(saved);
    }

    @Override
    public List<CounselingNoteResponse> getCounselingNotes(Long inquiryId) {
        return counselingNoteRepository
                .findByInquiryInquiryIdOrderByCreatedOnDesc(inquiryId)
                .stream().map(this::toCounselingResponse).collect(Collectors.toList());
    }

    @Override
    public AdmissionKpiResponse getKpi() {
        long total = inquiryRepository.countByDeletedFalse();
        long newCount = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.NEW);
        long converted = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.APPLICATION_SUBMITTED);
        long lost = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.LOST);
        long pending = inquiryRepository
                .findByDeletedFalseAndNextFollowUpDateLessThanEqualOrderByNextFollowUpDateAsc(LocalDate.now())
                .size();

        // Status breakdown
        Map<String, Long> breakdown = new HashMap<>();
        inquiryRepository.countByStatus()
                .forEach(row -> breakdown.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));

        long totalApps = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED)
                + applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW)
                + applicationRepository.countByStatus(ApplicationStatus.APPROVED)
                + applicationRepository.countByStatus(ApplicationStatus.REJECTED)
                + applicationRepository.countByStatus(ApplicationStatus.DRAFT);

        return AdmissionKpiResponse.builder()
                .totalInquiries(total)
                .newInquiries(newCount)
                .activeInquiries(total - converted - lost)
                .convertedInquiries(converted)
                .lostInquiries(lost)
                .pendingFollowUps(pending)
                .totalApplications(totalApps)
                .draftApplications(applicationRepository.countByStatus(ApplicationStatus.DRAFT))
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW))
                .approvedApplications(applicationRepository.countByStatus(ApplicationStatus.APPROVED))
                .rejectedApplications(applicationRepository.countByStatus(ApplicationStatus.REJECTED))
                .inquiryStatusBreakdown(breakdown)
                .build();
    }

        @Override
        public InquiryWorkspaceKpiResponse getWorkspaceKpi() {
        long newInquiries = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.NEW);
        long todayFollowUps = inquiryRepository
            .findByDeletedFalseAndNextFollowUpDateLessThanEqualOrderByNextFollowUpDateAsc(LocalDate.now())
                .stream()
                .filter(i -> LocalDate.now().equals(i.getNextFollowUpDate()))
                .count();
            long interested = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.INTERESTED);
            long admissionReady = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.INTERESTED);
            long futureProspects = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.CONTACTED)
                + inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.INTERESTED);
            long closed = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.LOST);
            return InquiryWorkspaceKpiResponse.builder()
                .newInquiries(newInquiries)
                .todaysFollowUps(todayFollowUps)
                .interested(interested)
                .admissionReady(admissionReady)
                .futureProspects(futureProspects)
                .closed(closed)
                .applicationsStarted(inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.APPLICATION_STARTED)
                        + inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.APPLICATION_SUBMITTED))
                .lostLeads(inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.LOST))
                .build();
            }

        @Override
        public InquiryQuickActionResponse getQuickActions() {
        LocalDate today = LocalDate.now();
        long overdue = followUpRepository.findOverdue(today).size();
        long dueToday = followUpRepository.findDueOnDate(today).size();
            long dueTomorrow = followUpRepository.findDueOnDate(today.plusDays(1)).size();
            long dueThisWeek = followUpRepository.findDueOnDate(today.plusDays(2)).size()
                + followUpRepository.findDueOnDate(today.plusDays(3)).size()
                + followUpRepository.findDueOnDate(today.plusDays(4)).size()
                + followUpRepository.findDueOnDate(today.plusDays(5)).size()
                + followUpRepository.findDueOnDate(today.plusDays(6)).size();
        long todaysCalls = followUpRepository.findDueOnDate(today).stream()
                .filter(f -> f.getFollowUpType() == FollowUpType.CALL)
                .count();
        long todaysMeetings = followUpRepository.findDueOnDate(today).stream()
                .filter(f -> f.getFollowUpType() == FollowUpType.WALK_IN)
                .count();
        return InquiryQuickActionResponse.builder()
                .overdue(overdue)
                .dueToday(dueToday)
                .dueTomorrow(dueTomorrow)
                .dueThisWeek(dueThisWeek)
                .todaysCalls(todaysCalls)
                .todaysMeetings(todaysMeetings)
                .overdueFollowUps(overdue)
                .admissionReady(inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.INTERESTED))
                .build();
            }

    @Override
    public Page<StaffSummaryResponse> getEligibleCounselors(String keyword, Pageable pageable) {
        requireViewLeads();
        List<StaffSummaryResponse> rows = responsibilityAssignmentRepository
                .findEligibleStaffByResponsibilityCode(COUNSELOR_RESPONSIBILITY_CODE, LocalDate.now())
                .stream()
                .map(ResponsibilityAssignment::getStaff)
                .filter(staff -> hasAdmissionsLeadAccess(staff, "VIEW"))
                .filter(staff -> {
                    if (!StringUtils.hasText(keyword)) {
                        return true;
                    }
                    String q = keyword.trim().toLowerCase();
                    return staffDisplayName(staff).toLowerCase().contains(q)
                            || (staff.getDesignation() != null && staff.getDesignation().toLowerCase().contains(q))
                            || (staff.getEmail() != null && staff.getEmail().toLowerCase().contains(q));
                })
                .distinct()
                .map(this::toStaffSummary)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), rows.size());
        if (start >= rows.size()) {
            return new PageImpl<>(List.of(), pageable, rows.size());
        }
        return new PageImpl<>(rows.subList(start, end), pageable, rows.size());
    }

    @Override
    public byte[] exportLeadsCsv(LeadSearchRequest request) {
        requireViewLeads();
        LeadSearchRequest enforced = applyVisibilityScope(request == null ? new LeadSearchRequest() : request);
        List<InquiryResponse> rows = inquiryRepository.findAll(InquirySpecification.filter(enforced)).stream()
                .sorted(Comparator.comparing(Inquiry::getCreatedOn, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Lead Number,Student Name,Parent Contact,Mobile,Class,Source,Status,Counselor,Next Follow-up,Created On\n");
        for (InquiryResponse row : rows) {
            csv.append(csvValue(row.getInquiryNumber())).append(',')
                    .append(csvValue(row.getStudentName())).append(',')
                    .append(csvValue(row.getParentContactName())).append(',')
                    .append(csvValue(row.getMobileNumber())).append(',')
                    .append(csvValue(row.getClassInterestedIn())).append(',')
                    .append(csvValue(row.getInquirySource() == null ? null : row.getInquirySource().name())).append(',')
                    .append(csvValue(row.getStatus() == null ? null : row.getStatus().name())).append(',')
                    .append(csvValue(row.getAssignedCounselorName())).append(',')
                    .append(csvValue(row.getNextFollowUpDate() == null ? null : row.getNextFollowUpDate().toString())).append(',')
                    .append(csvValue(row.getCreatedOn() == null ? null : row.getCreatedOn().toString()))
                    .append('\n');
        }
        auditWriteService.record(AuditEventType.EXPORT, "LEAD_EXPORT_CSV", "INQUIRY", null,
                "Exported " + rows.size() + " leads");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

            @Override
            public InquiryFullDetailResponse getFullDetail(Long inquiryId) {
            InquiryResponse inquiry = getById(inquiryId);
            var application = applicationRepository.findByInquiryId(inquiryId).orElse(null);
            return InquiryFullDetailResponse.builder()
                .inquiry(inquiry)
                .followUps(getFollowUps(inquiryId))
                .counselingNotes(getCounselingNotes(inquiryId))
                .timeline(getTimeline(inquiryId))
                .applicationId(application != null ? application.getApplicationId() : null)
                .applicationNumber(application != null ? application.getApplicationNumber() : null)
                .applicationStatus(application != null && application.getStatus() != null ? application.getStatus().name() : null)
                .studentId(application != null ? application.getStudentId() : null)
                .studentCode(null)
                .admissionNumber(null)
                .build();
            }

        @Override
        public List<InquiryTimelineItemResponse> getTimeline(Long inquiryId) {
        Inquiry inquiry = getInquiry(inquiryId);

            List<InquiryTimelineItemResponse> timeline = new ArrayList<>();
            boolean fromWebsite = inquiry.getInquirySource() == LeadSource.WEBSITE;
            timeline.add(InquiryTimelineItemResponse.builder()
                .eventType("LEAD_CREATED")
                .action("LEAD_CREATED")
                .title(fromWebsite ? "Enquiry received from website" : "Lead created")
                .description(fromWebsite
                    ? "Admission enquiry was submitted from the public website."
                    : "Lead was created in admissions CRM")
                .performedBy(inquiry.getCreatedBy())
                .performedOn(inquiry.getCreatedOn())
                .performedAt(inquiry.getCreatedOn())
                .build());

            followUpRepository.findByInquiryInquiryIdOrderByFollowUpDateDesc(inquiryId).forEach(fu ->
                timeline.add(InquiryTimelineItemResponse.builder()
                    .eventType("FOLLOW_UP")
                    .action("FOLLOW_UP")
                    .title("Follow-up: " + fu.getFollowUpType().name())
                    .description(fu.getRemarks())
                    .performedBy(fu.getCreatedBy())
                    .performedOn(fu.getFollowUpDate())
                    .performedAt(fu.getFollowUpDate())
                    .build())
            );

            counselingNoteRepository.findByInquiryInquiryIdOrderByCreatedOnDesc(inquiryId).forEach(note ->
                timeline.add(InquiryTimelineItemResponse.builder()
                    .eventType("COUNSELING_NOTE")
                    .action("COUNSELING_NOTE")
                    .title("Counseling note added")
                    .description(note.getNotes())
                    .performedBy(note.getCreatedBy())
                    .performedOn(note.getCreatedOn())
                    .performedAt(note.getCreatedOn())
                    .build())
            );

                    List<AuditLog> auditEvents = auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
                        "INQUIRY", String.valueOf(inquiryId));
                    for (AuditLog logEntry : auditEvents) {
                    timeline.add(InquiryTimelineItemResponse.builder()
                        .eventType(logEntry.getEventType() != null ? logEntry.getEventType().name() : "EVENT")
                        .action(logEntry.getAction())
                        .title(logEntry.getSummary())
                        .description(logEntry.getChanges())
                        .performedBy(logEntry.getActorUsername())
                        .performedAt(logEntry.getOccurredAt() != null
                            ? LocalDateTime.ofInstant(logEntry.getOccurredAt(), java.time.ZoneId.systemDefault())
                            : null)
                        .performedOn(logEntry.getOccurredAt() != null
                            ? LocalDateTime.ofInstant(logEntry.getOccurredAt(), java.time.ZoneId.systemDefault())
                            : null)
                        .build());
                    }

            timeline.sort(Comparator.comparing(InquiryTimelineItemResponse::getPerformedOn,
                Comparator.nullsLast(Comparator.reverseOrder())));
            return timeline;
            }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Inquiry getInquiry(Long inquiryId) {
        // Schema-per-tenant: Automatically scoped to current tenant schema
        return inquiryRepository.findByInquiryIdAndDeletedFalse(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));
    }

    private void mapRequest(InquiryRequest request, Inquiry inquiry) {
        String studentName = request.getName() == null ? null : request.getName().trim();
        inquiry.setName(studentName);
        inquiry.setStudentName(studentName);
        inquiry.setParentContactName(request.getParentContactName() == null ? null : request.getParentContactName().trim());
        inquiry.setMobileNumber(request.getMobileNumber() == null ? null : request.getMobileNumber().trim());
        inquiry.setEmail(request.getEmail());
        inquiry.setClassInterestedIn(request.getClassInterestedIn());
        inquiry.setAcademicYearId(request.getAcademicYearId());
        inquiry.setClassId(request.getClassId());
        inquiry.setAddress(request.getAddress());
        inquiry.setInquirySource(request.getInquirySource());
        inquiry.setReferredBy(request.getReferredBy() == null ? null : request.getReferredBy().trim());
        inquiry.setComments(request.getComments());
        if (request.getAssignedCounselorId() != null) {
            inquiry.setAssignedCounselorId(request.getAssignedCounselorId());
        }
        if (request.getNextFollowUpDate() != null) {
            inquiry.setNextFollowUpDate(request.getNextFollowUpDate());
        }
    }

    private InquiryResponse toResponse(Inquiry i) {
        return InquiryResponse.builder()
                .inquiryId(i.getInquiryId())
                .inquiryNumber(i.getInquiryNumber())
                .name(i.getStudentName())
                .studentName(i.getStudentName())
                .parentContactName(i.getParentContactName())
                .mobileNumber(i.getMobileNumber())
                .email(i.getEmail())
                .classInterestedIn(i.getClassInterestedIn())
                .academicYearId(i.getAcademicYearId())
                .classId(i.getClassId())
                .address(i.getAddress())
                .inquirySource(i.getInquirySource())
                .referredBy(i.getReferredBy())
                .comments(i.getComments())
                .assignedCounselorId(i.getAssignedCounselorId())
                .assignedCounselorName(resolveCounselorName(i.getAssignedCounselorId()))
                .status(i.getStatus())
                .lastFollowUpDate(i.getLastFollowUpDate())
                .lastFollowUpType(i.getLastFollowUpType())
                .nextFollowUpDate(i.getNextFollowUpDate())
                .createdOn(i.getCreatedOn())
                .createdBy(i.getCreatedBy())
                .build();
    }

    private void validateLeadRequest(InquiryRequest request) {
        if (request == null) {
            throw new BadRequestException("Lead request is required");
        }
        if (request.getInquirySource() == null) {
            throw new BadRequestException("Lead source is required");
        }
        if (request.getInquirySource() == LeadSource.REFERRAL
                && !StringUtils.hasText(request.getReferredBy())) {
            throw new BadRequestException("Referred by is required when source is Referral");
        }
    }

    private AcademicClass requireActiveClass(Long classId, Long academicYearId) {
        if (classId == null || academicYearId == null) {
            throw new BadRequestException("Academic year and class are required");
        }
        AcademicClass academicClass = classRepository.findByIdWithYear(classId)
                .orElseThrow(() -> new BadRequestException("Invalid class selected"));
        if (!Boolean.TRUE.equals(academicClass.getActive())) {
            throw new BadRequestException("Selected class is inactive");
        }
        if (academicClass.getAcademicYear() == null
                || !academicYearId.equals(academicClass.getAcademicYear().getAcademicYearId())) {
            throw new BadRequestException("Class does not belong to selected academic year");
        }
        return academicClass;
    }

    private Staff requireEligibleCounselor(Long counselorId) {
        if (counselorId == null) {
            throw new BadRequestException("Counselor is required");
        }
        Staff staff = staffRepository.findById(counselorId)
                .orElseThrow(() -> new BadRequestException("Counselor not found"));
        if (!Boolean.TRUE.equals(staff.getActive()) || staff.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BadRequestException("Counselor is not active");
        }
        boolean hasResponsibility = responsibilityAssignmentRepository.hasActiveResponsibilityCode(
                counselorId, COUNSELOR_RESPONSIBILITY_CODE, LocalDate.now());
        if (!hasResponsibility) {
            throw new BadRequestException("Selected staff is not assigned counselor responsibility");
        }
        if (!hasAdmissionsLeadAccess(staff, "VIEW")) {
            throw new BadRequestException("Selected counselor does not have admissions access");
        }
        return staff;
    }

    private void requireViewLeads() {
        if (hasElevatedRole()) {
            return;
        }
        User user = currentUser();
        Long orgId = OrganizationContext.getOrganizationId();
        if (user == null || orgId == null || !permissionService.hasPermission(user.getId(), orgId, RESOURCE_ADMISSIONS_LEADS, "VIEW")) {
            throw new AccessDeniedException("ADMISSIONS_LEADS:VIEW required");
        }
    }

    private void requireManageLeads() {
        if (hasElevatedRole()) {
            return;
        }
        User user = currentUser();
        Long orgId = OrganizationContext.getOrganizationId();
        if (user == null || orgId == null || !permissionService.hasPermission(user.getId(), orgId, RESOURCE_ADMISSIONS_LEADS, "MANAGE")) {
            throw new AccessDeniedException("ADMISSIONS_LEADS:MANAGE required");
        }
    }

    private LeadSearchRequest applyVisibilityScope(LeadSearchRequest request) {
        requireViewLeads();
        LeadSearchRequest effective = request == null ? new LeadSearchRequest() : request;

        if (hasElevatedRole()) {
            return effective;
        }

        Long staffId = currentStaffId();
        if (staffId == null) {
            return effective;
        }

        boolean hasCounselorRole = responsibilityAssignmentRepository.hasActiveResponsibilityCode(
                staffId, COUNSELOR_RESPONSIBILITY_CODE, LocalDate.now());
        boolean canViewAll = canManageLeads();
        boolean requestedAll = effective.getScope() != null && "ALL".equalsIgnoreCase(effective.getScope());

        if (hasCounselorRole && (!canViewAll || !requestedAll)) {
            effective.setCounselorId(staffId);
        }
        return effective;
    }

    private boolean canManageLeads() {
        if (hasElevatedRole()) {
            return true;
        }
        User user = currentUser();
        Long orgId = OrganizationContext.getOrganizationId();
        return user != null && orgId != null
                && permissionService.hasPermission(user.getId(), orgId, RESOURCE_ADMISSIONS_LEADS, "MANAGE");
    }

    private boolean hasAdmissionsLeadAccess(Staff staff, String privilege) {
        if (staff == null || staff.getUser() == null || staff.getUser().getId() == null) {
            return false;
        }
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            return false;
        }
        return permissionService.hasPermission(staff.getUser().getId(), orgId, RESOURCE_ADMISSIONS_LEADS, privilege);
    }

    private User currentUser() {
        String username = currentUsername();
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);
    }

    private Long currentUserId() {
        User user = currentUser();
        return user != null ? user.getId() : null;
    }

    private Long currentStaffId() {
        Long userId = currentUserId();
        if (userId == null) {
            return null;
        }
        return staffRepository.findByUser_Id(userId).map(Staff::getStaffId).orElse(null);
    }

    private boolean hasElevatedRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "SUPER_ADMIN".equals(a)
                        || "ORGANIZATION_OWNER".equals(a)
                        || "ORGANIZATION_ADMIN".equals(a));
    }

    private StaffSummaryResponse toStaffSummary(Staff staff) {
        return StaffSummaryResponse.builder()
                .staffId(staff.getStaffId())
                .staffCode(staff.getStaffCode())
                .fullName(staffDisplayName(staff))
                .email(staff.getEmail())
                .mobileNumber(staff.getMobileNumber())
                .photoUrl(staff.getPhotoUrl())
                .staffType(staff.getStaffType())
                .designation(staff.getDesignation())
                .employmentCategory(staff.getEmploymentCategory())
                .employmentStatus(staff.getEmploymentStatus())
                .joiningDate(staff.getJoiningDate())
                .active(staff.getActive())
                .build();
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r", " ").replace("\n", " ");
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private FollowUpResponse toFollowUpResponse(InquiryFollowUp f) {
        return FollowUpResponse.builder()
                .followUpId(f.getFollowUpId())
                .inquiryId(f.getInquiry().getInquiryId())
                .followUpType(f.getFollowUpType())
                .remarks(f.getRemarks())
                .statusAfter(f.getStatusAfter())
                .followUpDate(f.getFollowUpDate())
                .nextFollowUpDate(f.getNextFollowUpDate())
                .lifecycleStatus(f.getLifecycleStatus())
                .outcome(f.getOutcome())
                .completedOn(f.getCompletedOn())
                .completedBy(f.getCompletedBy())
                .leadName(f.getInquiry() != null ? f.getInquiry().getName() : null)
                .createdOn(f.getCreatedOn())
                .createdBy(f.getCreatedBy())
                .build();
    }

    private CounselingNoteResponse toCounselingResponse(CounselingNote n) {
        return CounselingNoteResponse.builder()
                .noteId(n.getNoteId())
                .inquiryId(n.getInquiry().getInquiryId())
                .studentRequirements(n.getStudentRequirements())
                .parentConcerns(n.getParentConcerns())
                .campusVisitInfo(n.getCampusVisitInfo())
                .recommendations(n.getRecommendations())
                .notes(n.getNotes())
                .createdOn(n.getCreatedOn())
                .createdBy(n.getCreatedBy())
                .build();
    }

    private String generateInquiryNumber() {
        String prefix = settingService.leadPrefix();
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String candidate;
        do {
            candidate = prefix + "-" + yearMonth + "-" + ThreadLocalRandom.current().nextLong(10000, 99999);
        } while (inquiryRepository.existsByInquiryNumber(candidate));
        return candidate;
    }

    private String generateApplicationNumber() {
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String candidate = "APP-" + yearMonth + "-" + ThreadLocalRandom.current().nextLong(10000, 99999);
        while (applicationRepository.existsByApplicationNumber(candidate)) {
            candidate = "APP-" + yearMonth + "-" + ThreadLocalRandom.current().nextLong(10000, 99999);
        }
        return candidate;
    }

    private ApplicationAdmissionResponse toApplicationResponse(ApplicationAdmission a) {
        return ApplicationAdmissionResponse.builder()
                .applicationId(a.getApplicationId())
                .applicationNumber(a.getApplicationNumber())
                .inquiryId(a.getInquiryId())
                .applicantName(a.getApplicantName())
                .dateOfBirth(a.getDateOfBirth())
                .gender(a.getGender())
                .applyingForClass(a.getApplyingForClass())
                .email(a.getEmail())
                .contactNumber(a.getContactNumber())
                .address(a.getAddress())
                .parentName(a.getParentName())
                .parentContact(a.getParentContact())
                .parentEmail(a.getParentEmail())
                .status(a.getStatus())
                .internalComments(a.getInternalComments())
                .uploadedDocuments(copiedApplicationDocuments(a))
                .createdOn(a.getCreatedOn())
                .createdBy(a.getCreatedBy())
                .build();
    }

    private List<String> copiedApplicationDocuments(ApplicationAdmission a) {
        try {
            List<String> docs = a.getUploadedDocuments();
            return docs == null || docs.isEmpty() ? List.of() : List.copyOf(docs);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private String resolveCounselorName(Long counselorId) {
        if (counselorId == null) {
            return null;
        }
        return staffRepository.findById(counselorId)
                .or(() -> staffRepository.findByUser_Id(counselorId))
                .map(this::staffDisplayName)
                .orElse(null);
    }

    private String staffDisplayName(Staff staff) {
        String last = staff.getLastName() == null ? "" : staff.getLastName().trim();
        return (staff.getFirstName() + " " + last).trim();
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
