package com.thinkerscave.common.admission.service;

import java.util.List;

import com.thinkerscave.common.admission.dto.InquiryRequest;
import com.thinkerscave.common.admission.dto.InquiryResponse;
import com.thinkerscave.common.admission.dto.PublicInquiryRequestDTO;
import com.thinkerscave.common.admission.dto.InquirySummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.validation.Valid;

public interface InquiryService {

	InquiryResponse saveOrUpdate(InquiryRequest request);

	List<InquiryResponse> getAll();

	Page<InquiryResponse> getAll(Pageable pageable);

	void delete(Long id);

	String createPublicInquiry(@Valid PublicInquiryRequestDTO request);

    InquirySummaryResponse getInquirySummary(Long inquiryId);

    void proceedToAdmission(Long inquiryId);

    void markLost(Long inquiryId);
    
    List<InquiryResponse> getDashboardInquiries(String tab, Long counselorId, java.time.LocalDate fromDate, java.time.LocalDate toDate);

}
