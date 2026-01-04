package com.thinkerscave.common.admission.service;

import java.util.List;

import com.thinkerscave.common.admission.dto.InquiryRequest;
import com.thinkerscave.common.admission.dto.InquiryResponse;
import com.thinkerscave.common.admission.dto.PublicInquiryRequestDTO;

import jakarta.validation.Valid;

public interface InquiryService {

	InquiryResponse saveOrUpdate(InquiryRequest request);

	List<InquiryResponse> getAll();

	void delete(Long id);

	String createPublicInquiry(@Valid PublicInquiryRequestDTO request);

}
