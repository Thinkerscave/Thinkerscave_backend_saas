package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnumOptionResponse {

    private String code;
    private String label;
}
