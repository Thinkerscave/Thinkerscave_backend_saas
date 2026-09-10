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
import com.thinkerscave.admission.repository.AdmissionsSettingRepository;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.repository.CounselingNoteRepository;
import com.thinkerscave.admission.repository.InquiryFollowUpRepository;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.admission.repository.LeadCounselorAssignmentRepository;
import com.thinkerscave.admission.entity.AdmissionsSetting;
import com.thinkerscave.admission.scheduling.FollowUpSlotPlanner;
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
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InquiryServiceImpl implements InquiryService {

    private static final String COUNSELOR_RESPONSIBILITY_CODE = "COUNSELOR";
    private static final String RESOURCE_ADMISSIONS_LEADS = "ADMISSIONS_LEADS";
    private static final String RESOURCE_ADMISSIONS_FOLLOW_UPS = "ADMISSIONS_FOLLOW_UPS";

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
    private final AdmissionsSettingRepository admissionsSettingRepository;
    private final StudentRepository studentRepository;
    private final FollowUpSlotPlanner followUpSlotPlanner;

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

        if (saved.getAssignedCounselorId() == null) {
            autoAssignCounselorIfEnabled(saved);
        }
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
            .findAll(InquirySpecification.filter(enforced), applyScopeSort(enforced, pageable))
                .map(this::toResponse);
    }

    @Override
    public Page<InquiryResponse> search(LeadSearchRequest request, Pageable pageable) {
        LeadSearchRequest enforced = applyVisibilityScope(request == null ? new LeadSearchRequest() : request);
        return inquiryRepository.findAll(InquirySpecification.filter(enforced), applyScopeSort(enforced, pageable))
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
        requireManageLeads();
        Inquiry inquiry = getInquiry(inquiryId);
        inquiry.setStatus(InquiryStatus.LOST);
        if (reason != null && !reason.isBlank()) {
            String existing = inquiry.getComments() == null ? "" : inquiry.getComments().trim();
            inquiry.setComments((existing.isEmpty() ? "" : existing + " | ") + "Lost Reason: " + reason.trim());
        }
        Inquiry saved = inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "LEAD_MARKED_LOST", "INQUIRY", String.valueOf(inquiryId),
            "Lead marked lost" + (StringUtils.hasText(reason) ? ": " + reason.trim() : ""));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InquiryResponse reopenLead(Long inquiryId) {
        requireManageLeads();
        Inquiry inquiry = getInquiry(inquiryId);
        if (inquiry.getStatus() != InquiryStatus.LOST) {
            throw new BadRequestException("Only a lost lead can be reopened");
        }
        // Re-enter the pipeline at CONTACTED — the lead was previously engaged, so NEW
        // would understate history. Lost reason/history is preserved in comments (never deleted).
        inquiry.setStatus(InquiryStatus.CONTACTED);
        Inquiry saved = inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.STATE_CHANGE, "LEAD_REOPENED", "INQUIRY", String.valueOf(inquiryId),
            "Lead reopened from Lost");
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
        ensureFirstFollowUpScheduled(saved, counselor);
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
        if (inquiry.getStatus() == InquiryStatus.LOST) {
            throw new BadRequestException("Cannot schedule a follow-up on a lost lead");
        }
        LocalDateTime scheduledAt = request.getFollowUpDate();
        if (scheduledAt == null && request.getNextFollowUpDate() != null) {
            scheduledAt = request.getNextFollowUpDate().atTime(10, 0);
        }
        if (scheduledAt == null) {
            throw new BadRequestException("Scheduled date & time is required");
        }

        InquiryFollowUp followUp = new InquiryFollowUp();
        followUp.setInquiry(inquiry);
        followUp.setFollowUpType(request.getFollowUpType());
        followUp.setRemarks(request.getRemarks());
        followUp.setFollowUpDate(scheduledAt);
        // Keep denormalized date aligned for queue queries that still read nextFollowUpDate.
        followUp.setNextFollowUpDate(scheduledAt.toLocalDate());
        followUp.setLifecycleStatus(FollowUpLifecycleStatus.SCHEDULED);
        followUp = followUpRepository.save(followUp);

        inquiry.setNextFollowUpDate(scheduledAt.toLocalDate());
        inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.UPDATE, "FOLLOW_UP_SCHEDULED", "INQUIRY", String.valueOf(inquiryId),
            "Follow-up scheduled: " + followUp.getFollowUpType() + " on " + scheduledAt);

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
        return resolveCounselorQueueOwnerId()
                .map(counselorId -> {
                    LocalDate today = LocalDate.now();
                    return followUpRepository.findTodayForCounselor(
                                    counselorId, today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                            .stream().map(this::toFollowUpResponse).collect(Collectors.toList());
                })
                .orElseGet(Collections::emptyList);
    }

    @Override
    public List<FollowUpResponse> getOverdueFollowUps() {
        return resolveCounselorQueueOwnerId()
                .map(counselorId -> followUpRepository.findOverdueForCounselor(
                                counselorId, LocalDate.now().atStartOfDay())
                        .stream().map(this::toFollowUpResponse).collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

    @Override
    public List<FollowUpResponse> getUpcomingFollowUps() {
        return resolveCounselorQueueOwnerId()
                .map(counselorId -> followUpRepository.findUpcomingForCounselor(
                                counselorId, LocalDate.now().plusDays(1).atStartOfDay())
                        .stream().map(this::toFollowUpResponse).collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

    @Override
    public List<FollowUpResponse> getCompletedFollowUps() {
        return resolveCounselorQueueOwnerId()
                .map(counselorId -> followUpRepository.findCompletedForCounselor(counselorId)
                        .stream().map(this::toFollowUpResponse).collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

    @Override
    @Transactional
    public FollowUpResponse updateFollowUp(Long followUpId, FollowUpRequest request) {
        InquiryFollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found: " + followUpId));
        requireCounselorOwnsFollowUp(followUp);

        if (request.getFollowUpType() != null) {
            followUp.setFollowUpType(request.getFollowUpType());
        }
        followUp.setRemarks(request.getRemarks());
        followUp.setStatusAfter(request.getStatusAfter());
        if (request.getFollowUpDate() != null) {
            followUp.setFollowUpDate(request.getFollowUpDate());
        }
        if (request.getNextFollowUpDate() != null
                && followUp.getLifecycleStatus() != FollowUpLifecycleStatus.COMPLETED
                && followUp.getLifecycleStatus() != FollowUpLifecycleStatus.CANCELLED) {
            followUp.setLifecycleStatus(FollowUpLifecycleStatus.SCHEDULED);
            followUp.setNextFollowUpDate(request.getNextFollowUpDate());
        }
        if (request.getFollowUpDate() != null) {
            followUp.setNextFollowUpDate(request.getFollowUpDate().toLocalDate());
            if (followUp.getLifecycleStatus() != FollowUpLifecycleStatus.COMPLETED
                    && followUp.getLifecycleStatus() != FollowUpLifecycleStatus.CANCELLED) {
                followUp.setLifecycleStatus(FollowUpLifecycleStatus.SCHEDULED);
            }
        }
        followUp = followUpRepository.save(followUp);

        Inquiry inquiry = followUp.getInquiry();
        inquiry.setNextFollowUpDate(followUp.getNextFollowUpDate());
        inquiryRepository.save(inquiry);
        auditWriteService.record(AuditEventType.UPDATE, "FOLLOW_UP_UPDATED", "INQUIRY", String.valueOf(inquiry.getInquiryId()),
            "Follow-up rescheduled");
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
        requireCounselorOwnsFollowUp(followUp);
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
        requireCounselorOwnsFollowUp(followUp);
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
        note.setSessionAt(request.getSessionAt() != null ? request.getSessionAt() : LocalDateTime.now());
        note.setMode(request.getMode());
        Long counselorStaffId = request.getCounselorStaffId() != null
                ? request.getCounselorStaffId()
                : currentStaffId();
        note.setCounselorStaffId(counselorStaffId);
        note.setStudentRequirements(request.getStudentRequirements());
        note.setParentConcerns(request.getParentConcerns());
        note.setCampusVisitInfo(request.getCampusVisitInfo());
        note.setRecommendations(request.getRecommendations());
        note.setNotes(request.getNotes());
        CounselingNote saved = counselingNoteRepository.save(note);

        auditWriteService.record(AuditEventType.UPDATE, "COUNSELING_ADDED", "INQUIRY", String.valueOf(inquiryId),
            "Counseling note added" + (counselorStaffId != null ? " by " + resolveCounselorName(counselorStaffId) : ""));

        // Complete only when this note is explicitly associated with a pending follow-up.
        InquiryFollowUp pending = resolvePendingFollowUp(inquiryId, request.getFollowUpId());
        if (pending != null) {
            pending.setLifecycleStatus(FollowUpLifecycleStatus.COMPLETED);
            pending.setCompletedOn(LocalDateTime.now());
            pending.setCompletedBy(currentUsername());
            pending.setOutcome("Completed via counseling note");
            followUpRepository.save(pending);
            inquiry.setLastFollowUpDate(note.getSessionAt());
            inquiry.setLastFollowUpType(pending.getFollowUpType());
            auditWriteService.record(AuditEventType.UPDATE, "FOLLOW_UP_COMPLETED", "INQUIRY", String.valueOf(inquiryId),
                "Follow-up completed via counseling note");
        }

        if (request.getLeadStatus() != null && request.getLeadStatus() != inquiry.getStatus()) {
            InquiryStatus previous = inquiry.getStatus();
            inquiry.setStatus(request.getLeadStatus());
            auditWriteService.record(AuditEventType.STATE_CHANGE, "LEAD_STATUS_CHANGED", "INQUIRY",
                    String.valueOf(inquiryId),
                    "Status changed from " + previous + " to " + request.getLeadStatus() + " via counseling");
        }

        if (request.getNextFollowUpAt() != null) {
            InquiryFollowUp next = new InquiryFollowUp();
            next.setInquiry(inquiry);
            next.setFollowUpType(request.getMode() != null ? request.getMode() : FollowUpType.CALL);
            next.setRemarks("Scheduled from counseling note");
            next.setFollowUpDate(request.getNextFollowUpAt());
            next.setNextFollowUpDate(request.getNextFollowUpAt().toLocalDate());
            next.setLifecycleStatus(FollowUpLifecycleStatus.SCHEDULED);
            followUpRepository.save(next);
            inquiry.setNextFollowUpDate(request.getNextFollowUpAt().toLocalDate());
            auditWriteService.record(AuditEventType.UPDATE, "FOLLOW_UP_SCHEDULED", "INQUIRY", String.valueOf(inquiryId),
                "Next follow-up scheduled from counseling: " + request.getNextFollowUpAt());
        } else if (pending != null) {
            // Pending FU was completed and no replacement scheduled — clear denormalized next date
            // unless another scheduled follow-up still exists.
            boolean stillScheduled = followUpRepository
                    .findByInquiryInquiryIdOrderByFollowUpDateDesc(inquiryId)
                    .stream()
                    .anyMatch(f -> f.getLifecycleStatus() == FollowUpLifecycleStatus.SCHEDULED
                            || f.getLifecycleStatus() == FollowUpLifecycleStatus.RESCHEDULED);
            if (!stillScheduled) {
                inquiry.setNextFollowUpDate(null);
            }
        }

        inquiryRepository.save(inquiry);
        return toCounselingResponse(saved);
    }

    private InquiryFollowUp resolvePendingFollowUp(Long inquiryId, Long followUpId) {
        if (followUpId == null) {
            return null;
        }
        InquiryFollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found: " + followUpId));
        if (!inquiryId.equals(followUp.getInquiry().getInquiryId())) {
            throw new BadRequestException("Follow-up does not belong to this lead");
        }
        if (followUp.getLifecycleStatus() == FollowUpLifecycleStatus.COMPLETED
                || followUp.getLifecycleStatus() == FollowUpLifecycleStatus.CANCELLED) {
            return null;
        }
        return followUp;
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
        Comparator<Inquiry> byScope = scopeSortComparator(enforced.getScope());
        List<InquiryResponse> rows = inquiryRepository.findAll(InquirySpecification.filter(enforced)).stream()
                .sorted(byScope)
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
            Long studentId = application != null ? application.getStudentId() : null;
            Student student = studentId != null ? studentRepository.findById(studentId).orElse(null) : null;
            return InquiryFullDetailResponse.builder()
                .inquiry(inquiry)
                .followUps(getFollowUps(inquiryId))
                .counselingNotes(getCounselingNotes(inquiryId))
                .timeline(getTimeline(inquiryId))
                .applicationId(application != null ? application.getApplicationId() : null)
                .applicationNumber(application != null ? application.getApplicationNumber() : null)
                .applicationStatus(application != null && application.getStatus() != null ? application.getStatus().name() : null)
                .studentId(studentId)
                .studentCode(student != null ? student.getStudentCode() : null)
                .admissionNumber(student != null ? student.getAdmissionNumber() : null)
                .build();
            }

        @Override
        public List<InquiryTimelineItemResponse> getTimeline(Long inquiryId) {
            return getTimeline(inquiryId, null, null, null);
        }

        @Override
        public List<InquiryTimelineItemResponse> getTimeline(Long inquiryId, String type, LocalDate from, LocalDate to) {
        Inquiry inquiry = getInquiry(inquiryId);

            List<InquiryTimelineItemResponse> timeline = new ArrayList<>();

            // Every real lead action (create, update, status change, assignment, follow-up,
            // counseling, application) is already written to the audit trail by the service
            // methods above — this is the single source of truth for Activity, so it is never
            // duplicated with a second, separately-built entity-based item (see ACTIVITY DATA
            // INTEGRITY requirement: only real actions should ever appear here).
            List<AuditLog> auditEvents = auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
                    "INQUIRY", String.valueOf(inquiryId));
            boolean hasCreateEvent = auditEvents.stream().anyMatch(e -> "LEAD_CREATED".equals(e.getAction()));

            for (AuditLog logEntry : auditEvents) {
                String action = logEntry.getAction();
                LocalDateTime when = logEntry.getOccurredAt() != null
                        ? LocalDateTime.ofInstant(logEntry.getOccurredAt(), java.time.ZoneId.systemDefault())
                        : null;
                timeline.add(InquiryTimelineItemResponse.builder()
                        .eventType(logEntry.getEventType() != null ? logEntry.getEventType().name() : "EVENT")
                        .action(action)
                        .category(activityCategory(action))
                        .title(activityTitle(action))
                        .description(logEntry.getSummary())
                        .performedBy(logEntry.getActorUsername())
                        .performedAt(when)
                        .performedOn(when)
                        .build());
            }

            // Fallback for leads created before the audit trail existed on this tenant.
            if (!hasCreateEvent) {
                boolean fromWebsite = inquiry.getInquirySource() == LeadSource.WEBSITE;
                timeline.add(InquiryTimelineItemResponse.builder()
                        .eventType("CREATE")
                        .action("LEAD_CREATED")
                        .category("LEAD")
                        .title("Lead Created")
                        .description(fromWebsite
                                ? "Admission enquiry was submitted from the public website."
                                : "Lead was created in admissions CRM.")
                        .performedBy(inquiry.getCreatedBy())
                        .performedOn(inquiry.getCreatedOn())
                        .performedAt(inquiry.getCreatedOn())
                        .build());
            }

            timeline.sort(Comparator.comparing(InquiryTimelineItemResponse::getPerformedOn,
                Comparator.nullsLast(Comparator.reverseOrder())));

            String typeFilter = StringUtils.hasText(type) ? type.trim().toUpperCase() : null;
            return timeline.stream()
                    .filter(item -> typeFilter == null
                            || typeFilter.equals(item.getCategory())
                            || typeFilter.equals(item.getAction()))
                    .filter(item -> from == null || item.getPerformedOn() == null
                            || !item.getPerformedOn().toLocalDate().isBefore(from))
                    .filter(item -> to == null || item.getPerformedOn() == null
                            || !item.getPerformedOn().toLocalDate().isAfter(to))
                    .toList();
            }

    /** Coarse category used to drive the Activity "type" filter dropdown. */
    private String activityCategory(String action) {
        if (action == null) {
            return "OTHER";
        }
        if (action.startsWith("FOLLOW_UP")) return "FOLLOW_UP";
        if (action.startsWith("COUNSELOR")) return "ASSIGNMENT";
        if (action.startsWith("COUNSELING")) return "COUNSELING";
        if (action.startsWith("APPLICATION") || action.startsWith("ENROLLMENT")) return "APPLICATION";
        if (action.contains("STATUS") || action.contains("LOST") || action.contains("REOPENED")) return "STATUS";
        if (action.startsWith("LEAD")) return "LEAD";
        return "OTHER";
    }

    /** Human-readable title for known lead activity action codes (falls back to a humanized form). */
    private String activityTitle(String action) {
        if (action == null) {
            return "Activity";
        }
        return switch (action) {
            case "LEAD_CREATED" -> "Lead Created";
            case "LEAD_UPDATED" -> "Lead Updated";
            case "LEAD_ARCHIVED" -> "Lead Archived";
            case "LEAD_STATUS_CHANGED" -> "Status Changed";
            case "LEAD_MARKED_LOST" -> "Marked Lost";
            case "LEAD_REOPENED" -> "Lead Reopened";
            case "COUNSELOR_ASSIGNED" -> "Counselor Assigned";
            case "COUNSELOR_REASSIGNED" -> "Counselor Reassigned";
            case "APPLICATION_STARTED" -> "Application Started";
            case "FOLLOW_UP_SCHEDULED" -> "Follow-up Scheduled";
            case "FOLLOW_UP_UPDATED" -> "Follow-up Updated";
            case "FOLLOW_UP_COMPLETED" -> "Follow-up Completed";
            case "FOLLOW_UP_CANCELLED" -> "Follow-up Cancelled";
            case "COUNSELING_ADDED" -> "Counseling Completed";
            case "LEAD_EXPORT_CSV" -> "Leads Exported";
            default -> java.util.Arrays.stream(action.split("_"))
                    .map(w -> w.isEmpty() ? w : w.charAt(0) + w.substring(1).toLowerCase())
                    .collect(Collectors.joining(" "));
        };
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
        // Progressive enrichment — only overwrite when the caller actually sends a value,
        // so partial "Edit Lead" section saves never wipe out previously enriched fields.
        if (request.getDateOfBirth() != null) inquiry.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) inquiry.setGender(request.getGender());
        if (request.getCurrentClass() != null) inquiry.setCurrentClass(request.getCurrentClass());
        if (request.getPreviousSchool() != null) inquiry.setPreviousSchool(request.getPreviousSchool());
        if (request.getAlternateMobileNumber() != null) inquiry.setAlternateMobileNumber(request.getAlternateMobileNumber());
        if (request.getContactRelationship() != null) inquiry.setContactRelationship(request.getContactRelationship());
        if (request.getCampusPreference() != null) inquiry.setCampusPreference(request.getCampusPreference());
        if (request.getTransportRequired() != null) inquiry.setTransportRequired(request.getTransportRequired());
        if (request.getHostelRequired() != null) inquiry.setHostelRequired(request.getHostelRequired());
        if (request.getOtherRequirements() != null) inquiry.setOtherRequirements(request.getOtherRequirements());
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
                .dateOfBirth(i.getDateOfBirth())
                .gender(i.getGender())
                .currentClass(i.getCurrentClass())
                .previousSchool(i.getPreviousSchool())
                .alternateMobileNumber(i.getAlternateMobileNumber())
                .contactRelationship(i.getContactRelationship())
                .campusPreference(i.getCampusPreference())
                .transportRequired(i.getTransportRequired())
                .hostelRequired(i.getHostelRequired())
                .otherRequirements(i.getOtherRequirements())
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

    /**
     * Backend-only sequential round-robin auto-assignment, run transactionally as part of
     * lead creation. Only activates when the tenant's Admissions setting "assignmentMode" is
     * ROUND_ROBIN (default is MANUAL, preserving existing behavior unless an admin opts in).
     * If there are no eligible counselors, the lead is simply left Unassigned — creation never fails.
     */
    private void autoAssignCounselorIfEnabled(Inquiry inquiry) {
        if (!"ROUND_ROBIN".equalsIgnoreCase(settingService.assignmentMode())) {
            return;
        }
        List<Staff> eligible = resolveEligibleCounselorsOrdered();
        if (eligible.isEmpty()) {
            return;
        }

        Long rawOrgId = OrganizationContext.getOrganizationId();
        final Long orgId = rawOrgId == null ? 0L : rawOrgId;
        // Pessimistic row lock held for the remainder of this transaction — concurrent lead
        // creations serialize on this row so each gets a distinct, non-colliding cursor value.
        AdmissionsSetting setting = admissionsSettingRepository.findByOrganizationIdForUpdate(orgId)
                .orElseGet(() -> admissionsSettingRepository.findByOrganizationId(orgId).orElse(null));
        if (setting == null) {
            return;
        }
        long index = setting.getNextCounselorIndex() == null ? 0L : setting.getNextCounselorIndex();
        setting.setNextCounselorIndex(index + 1);
        admissionsSettingRepository.save(setting);

        Staff chosen = eligible.get((int) (index % eligible.size()));
        inquiry.setAssignedCounselorId(chosen.getStaffId());
        inquiryRepository.save(inquiry);

        LeadCounselorAssignment history = new LeadCounselorAssignment();
        history.setInquiryId(inquiry.getInquiryId());
        history.setPreviousCounselorStaffId(null);
        history.setNewCounselorStaffId(chosen.getStaffId());
        history.setReason("Auto-assigned via round robin");
        history.setAssignedByUserId(null);
        history.setAssignedByUsername("System");
        history.setAssignedOn(LocalDateTime.now());
        history.setActive(true);
        leadCounselorAssignmentRepository.save(history);

        auditWriteService.record(AuditEventType.STATE_CHANGE, "COUNSELOR_ASSIGNED", "INQUIRY",
                String.valueOf(inquiry.getInquiryId()),
                "Counselor auto-assigned (round robin): " + staffDisplayName(chosen));
        ensureFirstFollowUpScheduled(inquiry, chosen);
    }

    /**
     * Creates the first scheduled follow-up when a counselor is assigned and the lead has none yet.
     * Slot selection prefers same business day when the counselor's relative load allows, otherwise
     * the next lighter business day — never beyond 2 business days (SLA).
     */
    private void ensureFirstFollowUpScheduled(Inquiry inquiry, Staff counselor) {
        if (inquiry == null || counselor == null || counselor.getStaffId() == null) {
            return;
        }
        if (inquiry.getStatus() == InquiryStatus.LOST) {
            return;
        }
        if (followUpRepository.existsByInquiryInquiryIdAndLifecycleStatus(
                inquiry.getInquiryId(), FollowUpLifecycleStatus.SCHEDULED)) {
            return;
        }

        Long counselorId = counselor.getStaffId();
        LocalDateTime scheduledAt = followUpSlotPlanner.suggestFirstFollowUpAt(day ->
                followUpRepository.countScheduledForCounselorBetween(
                        counselorId, day.atStartOfDay(), day.plusDays(1).atStartOfDay()));

        InquiryFollowUp followUp = new InquiryFollowUp();
        followUp.setInquiry(inquiry);
        followUp.setFollowUpType(FollowUpType.CALL);
        followUp.setRemarks("Auto-scheduled first follow-up after counselor assignment");
        followUp.setFollowUpDate(scheduledAt);
        followUp.setNextFollowUpDate(scheduledAt.toLocalDate());
        followUp.setLifecycleStatus(FollowUpLifecycleStatus.SCHEDULED);
        followUpRepository.save(followUp);

        inquiry.setNextFollowUpDate(scheduledAt.toLocalDate());
        inquiryRepository.save(inquiry);

        auditWriteService.record(AuditEventType.UPDATE, "FOLLOW_UP_SCHEDULED", "INQUIRY",
                String.valueOf(inquiry.getInquiryId()),
                "First follow-up auto-scheduled for " + scheduledAt.toLocalDate());
    }

    private List<Staff> resolveEligibleCounselorsOrdered() {
        return responsibilityAssignmentRepository
                .findEligibleStaffByResponsibilityCode(COUNSELOR_RESPONSIBILITY_CODE, LocalDate.now())
                .stream()
                .map(ResponsibilityAssignment::getStaff)
                .distinct()
                .sorted(Comparator.comparing(Staff::getStaffId))
                .toList();
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
            throw new BadRequestException("Selected staff is not assigned the COUNSELOR responsibility");
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

    /**
     * Counselor work-queue owner: linked staff with active COUNSELOR responsibility.
     * Org Admin / Owner / other users without that ownership get no queue data
     * (empty lists) — elevated roles do not broaden another counselor's queue.
     */
    private Optional<Long> resolveCounselorQueueOwnerId() {
        requireFollowUpsAccess();
        Long staffId = currentStaffId();
        if (staffId == null) {
            return Optional.empty();
        }
        boolean isCounselor = responsibilityAssignmentRepository.hasActiveResponsibilityCode(
                staffId, COUNSELOR_RESPONSIBILITY_CODE, LocalDate.now());
        if (!isCounselor) {
            return Optional.empty();
        }
        return Optional.of(staffId);
    }

    private void requireFollowUpsAccess() {
        if (hasElevatedRole()) {
            return;
        }
        User user = currentUser();
        Long orgId = OrganizationContext.getOrganizationId();
        if (user == null || orgId == null) {
            throw new AccessDeniedException("ADMISSIONS_FOLLOW_UPS:VIEW required");
        }
        boolean canViewFollowUps = permissionService.hasPermission(
                user.getId(), orgId, RESOURCE_ADMISSIONS_FOLLOW_UPS, "VIEW");
        boolean canViewLeads = permissionService.hasPermission(
                user.getId(), orgId, RESOURCE_ADMISSIONS_LEADS, "VIEW");
        if (!canViewFollowUps && !canViewLeads) {
            throw new AccessDeniedException("ADMISSIONS_FOLLOW_UPS:VIEW required");
        }
    }

    /**
     * Mutating a follow-up requires either the assigned counselor (COUNSELOR responsibility)
     * or an existing leads MANAGE privilege (elevated / permission architecture).
     * Queue list endpoints never broaden ownership via MANAGE.
     */
    private void requireCounselorOwnsFollowUp(InquiryFollowUp followUp) {
        Inquiry inquiry = followUp.getInquiry();
        Long staffId = currentStaffId();
        if (staffId != null
                && responsibilityAssignmentRepository.hasActiveResponsibilityCode(
                        staffId, COUNSELOR_RESPONSIBILITY_CODE, LocalDate.now())
                && inquiry != null
                && staffId.equals(inquiry.getAssignedCounselorId())) {
            return;
        }
        if (canManageLeads()) {
            return;
        }
        throw new AccessDeniedException("Only the assigned counselor can manage this follow-up");
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

    /**
     * Scope semantics (available to every Leads-page user — no role/privilege gating):
     * <ul>
     *   <li>MY — leads created by the current authenticated user ({@code createdBy})</li>
     *   <li>ALL — all non-deleted leads in the current tenant schema</li>
     * </ul>
     * When scope is omitted (other callers), no creator filter is applied.
     */
    private LeadSearchRequest applyVisibilityScope(LeadSearchRequest request) {
        requireViewLeads();
        LeadSearchRequest effective = request == null ? new LeadSearchRequest() : request;
        // Never trust a client-supplied createdBy — only the authenticated actor.
        effective.setCreatedBy(null);

        String scope = effective.getScope() == null ? "" : effective.getScope().trim();
        if ("MY".equalsIgnoreCase(scope)) {
            String username = currentUsername();
            if (!StringUtils.hasText(username)) {
                // No authenticated principal → empty MY result set (predicate that never matches).
                effective.setCreatedBy("__unauthenticated__");
            } else {
                effective.setCreatedBy(username.trim());
            }
        }
        return effective;
    }

    /** Server-side sort before pagination — ignores client sort for scope tabs. */
    private Pageable applyScopeSort(LeadSearchRequest request, Pageable pageable) {
        String scope = request == null || request.getScope() == null ? "" : request.getScope().trim();
        Sort sort;
        if ("ALL".equalsIgnoreCase(scope)) {
            // Next follow-up ASC, nulls last; then newest created first.
            sort = Sort.by(
                    Sort.Order.asc("nextFollowUpDate").nullsLast(),
                    Sort.Order.desc("createdOn")
            );
        } else if ("MY".equalsIgnoreCase(scope)) {
            sort = Sort.by(Sort.Order.desc("createdOn"));
        } else {
            // Unscoped callers keep their requested sort (fallback createdOn desc).
            sort = pageable.getSort().isSorted()
                    ? pageable.getSort()
                    : Sort.by(Sort.Order.desc("createdOn"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Comparator<Inquiry> scopeSortComparator(String scope) {
        if (scope != null && "ALL".equalsIgnoreCase(scope.trim())) {
            return Comparator
                    .comparing(Inquiry::getNextFollowUpDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Inquiry::getCreatedOn, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(Inquiry::getCreatedOn, Comparator.nullsLast(Comparator.reverseOrder()));
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
                .sessionAt(n.getSessionAt() != null ? n.getSessionAt() : n.getCreatedOn())
                .mode(n.getMode())
                .counselorStaffId(n.getCounselorStaffId())
                .counselorName(resolveCounselorName(n.getCounselorStaffId()))
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
