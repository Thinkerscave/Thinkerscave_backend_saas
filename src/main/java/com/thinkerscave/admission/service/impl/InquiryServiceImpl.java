package com.thinkerscave.admission.service.impl;

import com.thinkerscave.admission.dto.request.CounselingNoteRequest;
import com.thinkerscave.admission.dto.request.FollowUpRequest;
import com.thinkerscave.admission.dto.request.InquiryRequest;
import com.thinkerscave.admission.dto.response.AdmissionKpiResponse;
import com.thinkerscave.admission.dto.response.CounselingNoteResponse;
import com.thinkerscave.admission.dto.response.FollowUpResponse;
import com.thinkerscave.admission.dto.response.InquiryResponse;
import com.thinkerscave.admission.entity.CounselingNote;
import com.thinkerscave.admission.entity.Inquiry;
import com.thinkerscave.admission.entity.InquiryFollowUp;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.repository.CounselingNoteRepository;
import com.thinkerscave.admission.repository.InquiryFollowUpRepository;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.admission.service.InquiryService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    @Transactional
    public InquiryResponse create(InquiryRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        Inquiry inquiry = new Inquiry();
        mapRequest(request, inquiry);
        inquiry.setOrganizationId(orgId);
        inquiry.setStatus(InquiryStatus.NEW);
        inquiry.setDeleted(false);
        return toResponse(inquiryRepository.save(inquiry));
    }

    @Override
    @Transactional
    public InquiryResponse update(Long inquiryId, InquiryRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        Inquiry inquiry = getInquiry(inquiryId, orgId);
        mapRequest(request, inquiry);
        return toResponse(inquiryRepository.save(inquiry));
    }

    @Override
    public InquiryResponse getById(Long inquiryId) {
        return toResponse(getInquiry(inquiryId, OrganizationContext.getOrganizationId()));
    }

    @Override
    public Page<InquiryResponse> getAll(Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        return inquiryRepository
                .findByOrganizationIdAndDeletedFalseOrderByCreatedOnDesc(orgId, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<InquiryResponse> getByStatus(InquiryStatus status) {
        Long orgId = OrganizationContext.getOrganizationId();
        return inquiryRepository
                .findByOrganizationIdAndStatusAndDeletedFalseOrderByCreatedOnDesc(orgId, status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<InquiryResponse> getPendingFollowUps() {
        Long orgId = OrganizationContext.getOrganizationId();
        return inquiryRepository
                .findByOrganizationIdAndDeletedFalseAndNextFollowUpDateLessThanEqualOrderByNextFollowUpDateAsc(
                        orgId, LocalDate.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void softDelete(Long inquiryId) {
        Long orgId = OrganizationContext.getOrganizationId();
        Inquiry inquiry = getInquiry(inquiryId, orgId);
        inquiry.setDeleted(true);
        inquiryRepository.save(inquiry);
    }

    @Override
    @Transactional
    public InquiryResponse updateStatus(Long inquiryId, InquiryStatus newStatus) {
        Long orgId = OrganizationContext.getOrganizationId();
        Inquiry inquiry = getInquiry(inquiryId, orgId);
        inquiry.setStatus(newStatus);
        return toResponse(inquiryRepository.save(inquiry));
    }

    @Override
    @Transactional
    public InquiryResponse assignCounselor(Long inquiryId, Long counselorId) {
        Long orgId = OrganizationContext.getOrganizationId();
        Inquiry inquiry = getInquiry(inquiryId, orgId);
        inquiry.setAssignedCounselorId(counselorId);
        return toResponse(inquiryRepository.save(inquiry));
    }

    @Override
    @Transactional
    public FollowUpResponse addFollowUp(Long inquiryId, FollowUpRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        Inquiry inquiry = getInquiry(inquiryId, orgId);

        InquiryFollowUp followUp = new InquiryFollowUp();
        followUp.setInquiry(inquiry);
        followUp.setFollowUpType(request.getFollowUpType());
        followUp.setRemarks(request.getRemarks());
        followUp.setStatusAfter(request.getStatusAfter());
        followUp.setFollowUpDate(request.getFollowUpDate() != null ? request.getFollowUpDate() : LocalDateTime.now());
        followUp.setNextFollowUpDate(request.getNextFollowUpDate());
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
    @Transactional
    public CounselingNoteResponse addCounselingNote(Long inquiryId, CounselingNoteRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        Inquiry inquiry = getInquiry(inquiryId, orgId);

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
        Long orgId = OrganizationContext.getOrganizationId();

        long total = inquiryRepository.countByOrganizationIdAndDeletedFalse(orgId);
        long newCount = inquiryRepository.countByOrganizationIdAndStatusAndDeletedFalse(orgId, InquiryStatus.NEW);
        long converted = inquiryRepository.countByOrganizationIdAndStatusAndDeletedFalse(orgId, InquiryStatus.CONVERTED);
        long lost = inquiryRepository.countByOrganizationIdAndStatusAndDeletedFalse(orgId, InquiryStatus.LOST);
        long pending = inquiryRepository
                .findByOrganizationIdAndDeletedFalseAndNextFollowUpDateLessThanEqualOrderByNextFollowUpDateAsc(orgId, LocalDate.now())
                .size();

        // Status breakdown
        Map<String, Long> breakdown = new HashMap<>();
        inquiryRepository.countByStatusForOrg(orgId)
                .forEach(row -> breakdown.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));

        long totalApps = applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.SUBMITTED)
                + applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.UNDER_REVIEW)
                + applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.APPROVED)
                + applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.REJECTED)
                + applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.DRAFT);

        return AdmissionKpiResponse.builder()
                .totalInquiries(total)
                .newInquiries(newCount)
                .activeInquiries(total - converted - lost)
                .convertedInquiries(converted)
                .lostInquiries(lost)
                .pendingFollowUps(pending)
                .totalApplications(totalApps)
                .draftApplications(applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.DRAFT))
                .pendingApplications(applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.UNDER_REVIEW))
                .approvedApplications(applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.APPROVED))
                .rejectedApplications(applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.REJECTED))
                .inquiryStatusBreakdown(breakdown)
                .build();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Inquiry getInquiry(Long inquiryId, Long orgId) {
        return inquiryRepository.findByInquiryIdAndOrganizationIdAndDeletedFalse(inquiryId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));
    }

    private void mapRequest(InquiryRequest request, Inquiry inquiry) {
        inquiry.setName(request.getName());
        inquiry.setMobileNumber(request.getMobileNumber());
        inquiry.setEmail(request.getEmail());
        inquiry.setClassInterestedIn(request.getClassInterestedIn());
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
                .name(i.getName())
                .mobileNumber(i.getMobileNumber())
                .email(i.getEmail())
                .classInterestedIn(i.getClassInterestedIn())
                .address(i.getAddress())
                .inquirySource(i.getInquirySource())
                .referredBy(i.getReferredBy())
                .comments(i.getComments())
                .assignedCounselorId(i.getAssignedCounselorId())
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
}
