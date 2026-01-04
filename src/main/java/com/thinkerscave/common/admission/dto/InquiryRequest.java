package com.thinkerscave.common.admission.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryRequest {

	private Long inquiryId;
    private String name;
    private String mobileNumber;
    private String email;
    private String classInterested;
    private String address;
    private String inquirySource;
    private String referredBy;
    private String comments;
}

