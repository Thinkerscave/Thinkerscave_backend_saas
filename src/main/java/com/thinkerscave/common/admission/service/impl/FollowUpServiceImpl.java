package com.thinkerscave.common.admission.service.impl;

import com.thinkerscave.common.admission.domain.FollowUp;
import com.thinkerscave.common.admission.domain.Inquiry;
import com.thinkerscave.common.admission.dto.FollowUpRequest;
import com.thinkerscave.common.admission.dto.FollowUpResponse;
import com.thinkerscave.common.admission.enums.InquiryStatus;
import com.thinkerscave.common.admission.repository.FollowUpRepository;
import com.thinkerscave.common.admission.repository.InquiryRepository;
import com.thinkerscave.common.admission.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowUpServiceImpl implements FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final InquiryRepository inquiryRepository;

    @Override
    @Transactional
    public FollowUpResponse addFollowUp(Long inquiryId, FollowUpRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found"));

        if (inquiry.getStatus() == InquiryStatus.LOST || 
            inquiry.getStatus() == InquiryStatus.CONVERTED || 
            inquiry.getStatus() == InquiryStatus.CLOSED) {
            throw new RuntimeException("Cannot add follow-up for closed or converted inquiries");
        }

        FollowUp followUp = FollowUp.builder()
                .inquiry(inquiry)
                .followUpType(request.getFollowUpType())
                .remarks(request.getRemarks())
                .statusAfterFollowUp(request.getStatusAfterFollowUp())
                .followUpDate(LocalDateTime.now())
                .nextFollowUpDate(request.getNextFollowUpDate())
                .build();

        FollowUp savedFollowUp = followUpRepository.save(followUp);

        // Update Inquiry Status and Dates
        inquiry.setStatus(request.getStatusAfterFollowUp());
        inquiry.setLastFollowUpDate(LocalDateTime.now());
        inquiry.setLastFollowUpType(request.getFollowUpType());
        if (request.getStatusAfterFollowUp() != InquiryStatus.LOST && 
            request.getStatusAfterFollowUp() != InquiryStatus.CONVERTED) {
             inquiry.setNextFollowUpDate(request.getNextFollowUpDate());
        } else {
             inquiry.setNextFollowUpDate(null);
        }
        inquiryRepository.save(inquiry);

        return mapToResponse(savedFollowUp);
    }

    @Override
    public List<FollowUpResponse> getFollowUps(Long inquiryId) {
        return followUpRepository.findByInquiry_InquiryIdOrderByFollowUpDateDesc(inquiryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private FollowUpResponse mapToResponse(FollowUp followUp) {
        return FollowUpResponse.builder()
                .id(followUp.getId())
                .inquiryId(followUp.getInquiry().getInquiryId())
                .followUpType(followUp.getFollowUpType())
                .remarks(followUp.getRemarks())
                .statusAfterFollowUp(followUp.getStatusAfterFollowUp())
                .followUpDate(followUp.getFollowUpDate())
                .nextFollowUpDate(followUp.getNextFollowUpDate())
                .createdBy(followUp.getCreatedBy())
                .build();
    }
}
