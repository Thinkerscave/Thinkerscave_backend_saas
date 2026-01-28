package com.thinkerscave.common.admission.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.thinkerscave.common.admission.domain.Inquiry;
import com.thinkerscave.common.admission.dto.InquiryRequest;
import com.thinkerscave.common.admission.dto.InquiryResponse;
import com.thinkerscave.common.admission.dto.PublicInquiryRequestDTO;
import com.thinkerscave.common.admission.repository.InquiryRepository;
import com.thinkerscave.common.admission.service.InquiryService;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.security.SecurityUtil;

import com.thinkerscave.common.admission.dto.InquirySummaryResponse;
import com.thinkerscave.common.admission.enums.InquiryStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;

    // ================= SAVE / UPDATE =================
    
    @Override
	public String createPublicInquiry(PublicInquiryRequestDTO request) {

		Inquiry enquiry = Inquiry.builder().name(request.getName()).mobileNumber(request.getMobileNumber())
				.email(request.getEmail()).classInterestedIn(request.getClassInterestedIn())
				.address(request.getAddress()).inquirySource("WEBSITE").status(InquiryStatus.NEW).build();

		inquiryRepository.save(enquiry);

		return "Inquiry submitted successfully";
	}

    @Override
    public InquiryResponse saveOrUpdate(InquiryRequest request) {

        validateRequest(request);

        Inquiry inquiry;
        String userName=SecurityUtil.getCurrentUsername();

        // ---------- UPDATE ----------
        if (request.getInquiryId() != null) {
            inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(request.getInquiryId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Inquiry not found with id: " + request.getInquiryId())
                    );
        }
        // ---------- CREATE ----------
        else {
            inquiry = new Inquiry();
            inquiry.setStatus(InquiryStatus.NEW);
            inquiry.setIsDeleted(false);
            inquiry.setCreatedDate(new Date());
            inquiry.setCreatedBy(userName);
        }

        mapRequestToEntity(request, inquiry);
        inquiry.setLastModifiedDate(new Date());
        inquiry.setLastModifiedBy(userName);

        Inquiry saved = inquiryRepository.save(inquiry);
        return mapToResponse(saved);
    }

    // ================= GET ALL =================

    @Override
    @Transactional(readOnly = true)
    public List<InquiryResponse> getAll() {

        return inquiryRepository.findAllByIsDeletedFalseOrderByCreatedDateDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ================= DELETE (SOFT) =================

    @Override
    public void delete(Long id) {

        Inquiry inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Inquiry not found with id: " + id)
                );

        inquiry.setIsDeleted(true);
        inquiry.setLastModifiedDate(new Date());
        inquiry.setLastModifiedBy(SecurityUtil.getCurrentUsername());
        inquiryRepository.save(inquiry);
    }

    // ================= VALIDATIONS =================

    private void validateRequest(InquiryRequest request) {

        if (!StringUtils.hasText(request.getName()) || request.getName().length() < 2) {
            throw new BadRequestException("Name must contain at least 2 characters");
        }

        if (!StringUtils.hasText(request.getMobileNumber())
                || !request.getMobileNumber().matches("^[6-9]\\d{9}$")) {
            throw new BadRequestException("Invalid mobile number");
        }

        if (!StringUtils.hasText(request.getEmail())) {
            throw new BadRequestException("Email is required");
        }

        if (!StringUtils.hasText(request.getClassInterested())) {
            throw new BadRequestException("Class interested is required");
        }

        if (!StringUtils.hasText(request.getInquirySource())) {
            throw new BadRequestException("Inquiry source is required");
        }

        if (!StringUtils.hasText(request.getAddress()) || request.getAddress().length() < 5) {
            throw new BadRequestException("Address must contain at least 5 characters");
        }
    }

    // ================= MAPPERS =================

    private void mapRequestToEntity(InquiryRequest request, Inquiry inquiry) {

        inquiry.setName(request.getName());
        inquiry.setMobileNumber(request.getMobileNumber());
        inquiry.setEmail(request.getEmail());
        inquiry.setClassInterestedIn(request.getClassInterested());
        inquiry.setAddress(request.getAddress());
        inquiry.setInquirySource(request.getInquirySource());
        inquiry.setReferredBy(request.getReferredBy());
        inquiry.setComments(request.getComments());
    }

    private InquiryResponse mapToResponse(Inquiry inquiry) {

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
                .status(inquiry.getStatus().name())
                .assignedCounselor("Counselor")
                .lastFollowUpDate(inquiry.getLastFollowUpDate())
                .lastFollowUpType(inquiry.getLastFollowUpType() != null ? inquiry.getLastFollowUpType().name() : null)
                .nextFollowUpDate(inquiry.getNextFollowUpDate())
//                .createdAt(inquiry.getCreatedAt())
                .build();
    }

    @Override
    public InquirySummaryResponse getInquirySummary(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
        
        return InquirySummaryResponse.builder()
                .inquiryId(inquiry.getInquiryId())
                .name(inquiry.getName())
                .mobileNumber(inquiry.getMobileNumber())
                .classInterested(inquiry.getClassInterestedIn())
                .status(inquiry.getStatus())
                .assignedCounselorId(inquiry.getAssignedCounselorId())
                .lastFollowUpDate(inquiry.getLastFollowUpDate())
                .nextFollowUpDate(inquiry.getNextFollowUpDate())
                .comments(inquiry.getComments())
                .build();
    }

    @Override
    public void proceedToAdmission(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));

        if(inquiry.getStatus() == InquiryStatus.LOST || inquiry.getStatus() == InquiryStatus.CLOSED) {
             throw new BadRequestException("Cannot proceed closed/lost inquiry to admission");
        }

        inquiry.setStatus(InquiryStatus.READY_FOR_ADMISSION);
        inquiry.setNextFollowUpDate(null); // Lock follow-ups usually implies no future follow ups? Or just explicit status.
        inquiryRepository.save(inquiry);
    }

    @Override
    public void markLost(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findByInquiryIdAndIsDeletedFalse(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
        
        inquiry.setStatus(InquiryStatus.LOST);
        inquiry.setNextFollowUpDate(null);
        inquiryRepository.save(inquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InquiryResponse> getDashboardInquiries(String tab, Long counselorId, LocalDate fromDate, LocalDate toDate) {
        List<Inquiry> inquiries;
        
        if (counselorId == null) {
            // If needed, handle admin view or throw exception. Assume required for now or fetch all if admin logic exists.
            // For now, let's assume filtering primarily by counselor if provided, otherwise generic filter.
            // But BFS says "Inquiry assigned to Counselor".
            // If simple implementation:
             inquiries = inquiryRepository.findAllByIsDeletedFalseOrderByCreatedDateDesc(); // Fallback
        } else {
             inquiries = inquiryRepository.findByAssignedCounselorIdAndIsDeletedFalse(counselorId);
        }
        
        // Filtering in memory for complex logic or specific tabs if repository methods are limited
        // or using the repository methods I added.
        
        switch (tab.toUpperCase()) {
            case "TODAY":
                inquiries = inquiryRepository.findByAssignedCounselorIdAndNextFollowUpDateAndIsDeletedFalse(counselorId, LocalDate.now());
                break;
            case "OVERDUE":
                inquiries = inquiryRepository.findByAssignedCounselorIdAndNextFollowUpDateBeforeAndIsDeletedFalse(counselorId, LocalDate.now());
                break;
            case "UPCOMING":
                inquiries = inquiryRepository.findByAssignedCounselorIdAndNextFollowUpDateAfterAndIsDeletedFalse(counselorId, LocalDate.now());
                break;
            case "NEW":
                 inquiries = inquiryRepository.findByAssignedCounselorIdAndStatusAndIsDeletedFalse(counselorId, InquiryStatus.NEW);
                 break;
            case "READY":
                 inquiries = inquiryRepository.findByAssignedCounselorIdAndStatusAndIsDeletedFalse(counselorId, InquiryStatus.READY_FOR_ADMISSION);
                 break;
            case "LOST":
                 inquiries = inquiryRepository.findByAssignedCounselorIdAndStatusAndIsDeletedFalse(counselorId, InquiryStatus.LOST);
                 break;
            default:
                 // "ALL" or invalid tab
                 // inquiries already fetched above or filtered
                 break;
        }

        return inquiries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}

