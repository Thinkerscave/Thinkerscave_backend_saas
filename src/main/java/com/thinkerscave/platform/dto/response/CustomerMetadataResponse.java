package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerMetadataResponse {

    private List<EnumOptionResponse> statuses;
    private List<EnumOptionResponse> customerTypes;
    private List<EnumOptionResponse> preferredCommunications;
}
