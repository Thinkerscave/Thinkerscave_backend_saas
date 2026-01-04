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
				.address(request.getAddress()).inquirySource("WEBSITE").status("NEW").build();

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
            inquiry.setStatus("NEW");
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
                .status(inquiry.getStatus())
                .assignedCounselor("Counselor")
//                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}

