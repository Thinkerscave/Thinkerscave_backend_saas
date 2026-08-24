package com.thinkerscave.admission.service.impl;

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
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.FollowUpLifecycleStatus;
import com.thinkerscave.admission.enums.FollowUpType;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.repository.CounselingNoteRepository;
import com.thinkerscave.admission.repository.InquiryFollowUpRepository;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.admission.service.AdmissionsSettingService;
import com.thinkerscave.admission.service.InquiryService;
import com.thinkerscave.admission.specification.InquirySpecification;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final InquiryRepository inquiryRepository;
    private final InquiryFollowUpRepository followUpRepository;
    private final CounselingNoteRepository counselingNoteRepository;
    private final ApplicationAdmissionRepository applicationRepository;
    private final AdmissionsSettingService settingService;
    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public InquiryResponse create(InquiryRequest request) {
        // Schema-per-tenant: No need to set organizationId
        if (request.getMobileNumber() != null
                && inquiryRepository.existsByMobileNumberAndDeletedFalse(request.getMobileNumber().trim())) {
            throw new BadRequestException("A lead with this mobile number already exists");
        }
        Inquiry inquiry = new Inquiry();
        mapRequest(request, inquiry);
        inquiry.setStatus(InquiryStatus.NEW);
        inquiry.setDeleted(false);
        inquiry.setInquiryNumber(generateInquiryNumber());
        return toResponse(inquiryRepository.save(inquiry));
    }

    @Override
    @Transactional
    public InquiryResponse update(Long inquiryId, InquiryRequest request) {
        Inquiry inquiry = getInquiry(inquiryId);
        mapRequest(request, inquiry);
        return toResponse(inquiryRepository.save(inquiry));
    }

    @Override
    public InquiryResponse getById(Long inquiryId) {
        return toResponse(getInquiry(inquiryId));
    }

    @Override
    public Page<InquiryResponse> getAll(Pageable pageable) {
        // Schema context automatically isolates to tenant
        return inquiryRepository
                .findByDeletedFalseOrderByCreatedOnDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<InquiryResponse> search(LeadSearchRequest request, Pageable pageable) {
        // Schema-per-tenant: Automatically scoped to current tenant schema
        return inquiryRepository.findAll(InquirySpecification.filter(request), pageable)
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
        Inquiry inquiry = getInquiry(inquiryId);
        inquiry.setDeleted(true);
        inquiryRepository.save(inquiry);
    }

    @Override
    @Transactional
    public InquiryResponse updateStatus(Long inquiryId, InquiryStatus newStatus) {
        Inquiry inquiry = getInquiry(inquiryId);
        inquiry.setStatus(newStatus);
        return toResponse(inquiryRepository.save(inquiry));
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
        return toResponse(inquiryRepository.save(inquiry));
    }

    @Override
    @Transactional
    public InquiryResponse assignCounselor(Long inquiryId, Long counselorId) {
        Inquiry inquiry = getInquiry(inquiryId);
        inquiry.setAssignedCounselorId(counselorId);
        return toResponse(inquiryRepository.save(inquiry));
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

        if (inquiry.getStatus() == InquiryStatus.LOST || inquiry.getStatus() == InquiryStatus.CLOSED) {
            throw new BadRequestException("Lost or closed inquiries cannot be converted to applications");
        }

        ApplicationAdmission app = new ApplicationAdmission();
        app.setInquiryId(inquiry.getInquiryId());
        app.setApplicationNumber(generateApplicationNumber());
        app.setApplicantName(inquiry.getName());
        app.setApplyingForClass(inquiry.getClassInterestedIn());
        app.setAcademicYearId(inquiry.getAcademicYearId());
        app.setClassId(inquiry.getClassId());
        app.setEmail(inquiry.getEmail());
        app.setContactNumber(inquiry.getMobileNumber());
        app.setAddress(inquiry.getAddress());
        app.setParentContact(inquiry.getMobileNumber());
        app.setInternalComments(inquiry.getComments());
        app.setStatus(ApplicationStatus.DRAFT);
        ApplicationAdmission saved = applicationRepository.save(app);

        inquiry.setStatus(InquiryStatus.APPLICATION_STARTED);
        inquiryRepository.save(inquiry);
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
        return toFollowUpResponse(followUpRepository.save(followUp));
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
        return toFollowUpResponse(followUpRepository.save(followUp));
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

        return toCounselingResponse(counselingNoteRepository.save(note));
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
        long converted = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.CONVERTED);
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
            long admissionReady = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.READY_FOR_ADMISSION);
            long futureProspects = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.CONTACTED)
                + inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.FOLLOW_UP_REQUIRED)
                + inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.FOLLOW_UP);
            long closed = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.CLOSED)
                + inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.LOST);
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
                .admissionReady(inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.READY_FOR_ADMISSION))
                .build();
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
            timeline.add(InquiryTimelineItemResponse.builder()
                .eventType("LEAD_CREATED")
                .action("LEAD_CREATED")
                .title("Lead created")
                .description("Lead was created in admissions CRM")
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
        inquiry.setName(request.getName());
        inquiry.setMobileNumber(request.getMobileNumber());
        inquiry.setEmail(request.getEmail());
        inquiry.setClassInterestedIn(request.getClassInterestedIn());
        inquiry.setAcademicYearId(request.getAcademicYearId());
        inquiry.setClassId(request.getClassId());
        inquiry.setAddress(request.getAddress());
        inquiry.setInquirySource(request.getInquirySource());
        inquiry.setReferredBy(request.getReferredBy());
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
                .name(i.getName())
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
